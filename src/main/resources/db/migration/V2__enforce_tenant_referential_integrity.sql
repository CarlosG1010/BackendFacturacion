IF OBJECT_ID(N'dbo.tenants', N'U') IS NULL
   OR OBJECT_ID(N'dbo.usuarios', N'U') IS NULL
   OR OBJECT_ID(N'dbo.clientes', N'U') IS NULL
   OR OBJECT_ID(N'dbo.categorias', N'U') IS NULL
   OR OBJECT_ID(N'dbo.marcas', N'U') IS NULL
   OR OBJECT_ID(N'dbo.productos', N'U') IS NULL
   OR OBJECT_ID(N'dbo.comprobantes', N'U') IS NULL
   OR OBJECT_ID(N'dbo.comprobantes_detalle', N'U') IS NULL
   OR OBJECT_ID(N'dbo.control_series', N'U') IS NULL
   OR OBJECT_ID(N'dbo.sunat_lotes', N'U') IS NULL
   OR OBJECT_ID(N'dbo.auditoria_empresa', N'U') IS NULL
   OR OBJECT_ID(N'dbo.auditoria_global', N'U') IS NULL
BEGIN
    THROW 51001, 'Faltan tablas del esquema base. Aplique primero la migracion inicial del sistema.', 1;
END;

IF COL_LENGTH('dbo.tenants', 'version') IS NULL
BEGIN
    ALTER TABLE dbo.tenants ADD version BIGINT NOT NULL
        CONSTRAINT df_tenants_version DEFAULT 0;
END;

IF COL_LENGTH('dbo.comprobantes_detalle', 'tenant_id') IS NULL
BEGIN
    ALTER TABLE dbo.comprobantes_detalle ADD tenant_id UNIQUEIDENTIFIER NULL;
END;

UPDATE detail
SET tenant_id = invoice.tenant_id
FROM dbo.comprobantes_detalle detail
JOIN dbo.comprobantes invoice ON invoice.id = detail.comprobante_id
WHERE detail.tenant_id IS NULL;

IF EXISTS (SELECT 1 FROM dbo.comprobantes_detalle WHERE tenant_id IS NULL)
BEGIN
    THROW 51002, 'No se pudo inferir el tenant de todos los detalles de comprobante.', 1;
END;

ALTER TABLE dbo.comprobantes_detalle ALTER COLUMN tenant_id UNIQUEIDENTIFIER NOT NULL;

IF COL_LENGTH('dbo.sunat_lotes', 'documentos_json') IS NOT NULL
BEGIN
    IF EXISTS (SELECT 1 FROM dbo.sunat_lotes WHERE tipo_lote NOT IN ('RESUMEN', 'BAJA'))
    BEGIN
        THROW 51006, 'Hay tipos de lote no soportados; no se eliminara documentos_json.', 1;
    END;

    IF EXISTS (
        SELECT 1
        FROM dbo.sunat_lotes batch
        WHERE batch.documentos_json IS NOT NULL AND ISJSON(batch.documentos_json) <> 1
    )
    BEGIN
        THROW 51003, 'Hay documentos_json de lote con formato invalido; no se eliminaran referencias.', 1;
    END;

    IF EXISTS (
        SELECT 1
        FROM dbo.sunat_lotes batch
        CROSS APPLY OPENJSON(batch.documentos_json) item
        LEFT JOIN dbo.comprobantes invoice
            ON invoice.id = TRY_CONVERT(UNIQUEIDENTIFIER, item.value)
           AND invoice.tenant_id = batch.tenant_id
        WHERE invoice.id IS NULL
    )
    BEGIN
        THROW 51004, 'Hay referencias de lote sin comprobante del mismo tenant; corrija los datos antes de migrar.', 1;
    END;

    UPDATE invoice
    SET lote_resumen_id = batch.id
    FROM dbo.sunat_lotes batch
    CROSS APPLY OPENJSON(batch.documentos_json) item
    JOIN dbo.comprobantes invoice
      ON invoice.id = TRY_CONVERT(UNIQUEIDENTIFIER, item.value)
     AND invoice.tenant_id = batch.tenant_id
    WHERE batch.tipo_lote = 'RESUMEN'
      AND (invoice.lote_resumen_id IS NULL OR invoice.lote_resumen_id = batch.id);

    UPDATE invoice
    SET lote_baja_id = batch.id
    FROM dbo.sunat_lotes batch
    CROSS APPLY OPENJSON(batch.documentos_json) item
    JOIN dbo.comprobantes invoice
      ON invoice.id = TRY_CONVERT(UNIQUEIDENTIFIER, item.value)
     AND invoice.tenant_id = batch.tenant_id
    WHERE batch.tipo_lote = 'BAJA'
      AND (invoice.lote_baja_id IS NULL OR invoice.lote_baja_id = batch.id);

    IF EXISTS (
        SELECT 1
        FROM dbo.sunat_lotes batch
        CROSS APPLY OPENJSON(batch.documentos_json) item
        JOIN dbo.comprobantes invoice
          ON invoice.id = TRY_CONVERT(UNIQUEIDENTIFIER, item.value)
         AND invoice.tenant_id = batch.tenant_id
        WHERE (batch.tipo_lote = 'RESUMEN' AND invoice.lote_resumen_id <> batch.id)
           OR (batch.tipo_lote = 'BAJA' AND invoice.lote_baja_id <> batch.id)
    )
    BEGIN
        THROW 51005, 'Un comprobante esta asignado a otro lote distinto del guardado en documentos_json.', 1;
    END;

    ALTER TABLE dbo.sunat_lotes DROP COLUMN documentos_json;
END;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'uk_usuarios_tenant_id'
               AND object_id = OBJECT_ID(N'dbo.usuarios'))
    CREATE UNIQUE INDEX uk_usuarios_tenant_id ON dbo.usuarios(tenant_id, id);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'uk_clientes_tenant_id'
               AND object_id = OBJECT_ID(N'dbo.clientes'))
    CREATE UNIQUE INDEX uk_clientes_tenant_id ON dbo.clientes(tenant_id, id);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'uk_categorias_tenant_id'
               AND object_id = OBJECT_ID(N'dbo.categorias'))
    CREATE UNIQUE INDEX uk_categorias_tenant_id ON dbo.categorias(tenant_id, id);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'uk_marcas_tenant_id'
               AND object_id = OBJECT_ID(N'dbo.marcas'))
    CREATE UNIQUE INDEX uk_marcas_tenant_id ON dbo.marcas(tenant_id, id);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'uk_productos_tenant_id'
               AND object_id = OBJECT_ID(N'dbo.productos'))
    CREATE UNIQUE INDEX uk_productos_tenant_id ON dbo.productos(tenant_id, id);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'uk_comprobantes_tenant_id'
               AND object_id = OBJECT_ID(N'dbo.comprobantes'))
    CREATE UNIQUE INDEX uk_comprobantes_tenant_id ON dbo.comprobantes(tenant_id, id);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'uk_sunat_lotes_tenant_id'
               AND object_id = OBJECT_ID(N'dbo.sunat_lotes'))
    CREATE UNIQUE INDEX uk_sunat_lotes_tenant_id ON dbo.sunat_lotes(tenant_id, id);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'uk_series_tenant_tipo'
               AND object_id = OBJECT_ID(N'dbo.control_series'))
    CREATE UNIQUE INDEX uk_series_tenant_tipo
        ON dbo.control_series(tenant_id, tipo_comprobante, serie);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'uk_sunat_lotes_correlativo'
               AND object_id = OBJECT_ID(N'dbo.sunat_lotes'))
    CREATE UNIQUE INDEX uk_sunat_lotes_correlativo
        ON dbo.sunat_lotes(tenant_id, tipo_lote, fecha_documentos, correlativo);

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'fk_usuarios_tenant')
    ALTER TABLE dbo.usuarios ADD CONSTRAINT fk_usuarios_tenant
        FOREIGN KEY (tenant_id) REFERENCES dbo.tenants(id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'fk_clientes_tenant')
    ALTER TABLE dbo.clientes ADD CONSTRAINT fk_clientes_tenant
        FOREIGN KEY (tenant_id) REFERENCES dbo.tenants(id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'fk_categorias_tenant')
    ALTER TABLE dbo.categorias ADD CONSTRAINT fk_categorias_tenant
        FOREIGN KEY (tenant_id) REFERENCES dbo.tenants(id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'fk_marcas_tenant')
    ALTER TABLE dbo.marcas ADD CONSTRAINT fk_marcas_tenant
        FOREIGN KEY (tenant_id) REFERENCES dbo.tenants(id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'fk_productos_tenant')
    ALTER TABLE dbo.productos ADD CONSTRAINT fk_productos_tenant
        FOREIGN KEY (tenant_id) REFERENCES dbo.tenants(id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'fk_comprobantes_tenant')
    ALTER TABLE dbo.comprobantes ADD CONSTRAINT fk_comprobantes_tenant
        FOREIGN KEY (tenant_id) REFERENCES dbo.tenants(id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'fk_comprobantes_detalle_tenant')
    ALTER TABLE dbo.comprobantes_detalle ADD CONSTRAINT fk_comprobantes_detalle_tenant
        FOREIGN KEY (tenant_id) REFERENCES dbo.tenants(id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'fk_control_series_tenant')
    ALTER TABLE dbo.control_series ADD CONSTRAINT fk_control_series_tenant
        FOREIGN KEY (tenant_id) REFERENCES dbo.tenants(id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'fk_sunat_lotes_tenant')
    ALTER TABLE dbo.sunat_lotes ADD CONSTRAINT fk_sunat_lotes_tenant
        FOREIGN KEY (tenant_id) REFERENCES dbo.tenants(id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'fk_auditoria_empresa_tenant')
    ALTER TABLE dbo.auditoria_empresa ADD CONSTRAINT fk_auditoria_empresa_tenant
        FOREIGN KEY (tenant_id) REFERENCES dbo.tenants(id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'fk_auditoria_global_tenant')
    ALTER TABLE dbo.auditoria_global ADD CONSTRAINT fk_auditoria_global_tenant
        FOREIGN KEY (tenant_id) REFERENCES dbo.tenants(id);

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'fk_productos_categoria_tenant')
    ALTER TABLE dbo.productos ADD CONSTRAINT fk_productos_categoria_tenant
        FOREIGN KEY (tenant_id, categoria_id) REFERENCES dbo.categorias(tenant_id, id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'fk_productos_marca_tenant')
    ALTER TABLE dbo.productos ADD CONSTRAINT fk_productos_marca_tenant
        FOREIGN KEY (tenant_id, marca_id) REFERENCES dbo.marcas(tenant_id, id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'fk_comprobantes_cliente_tenant')
    ALTER TABLE dbo.comprobantes ADD CONSTRAINT fk_comprobantes_cliente_tenant
        FOREIGN KEY (tenant_id, cliente_id) REFERENCES dbo.clientes(tenant_id, id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'fk_comprobantes_usuario_tenant')
    ALTER TABLE dbo.comprobantes ADD CONSTRAINT fk_comprobantes_usuario_tenant
        FOREIGN KEY (tenant_id, usuario_id) REFERENCES dbo.usuarios(tenant_id, id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'fk_comprobantes_origen_tenant')
    ALTER TABLE dbo.comprobantes ADD CONSTRAINT fk_comprobantes_origen_tenant
        FOREIGN KEY (tenant_id, comprobante_origen_id) REFERENCES dbo.comprobantes(tenant_id, id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'fk_detalles_comprobante_tenant')
    ALTER TABLE dbo.comprobantes_detalle ADD CONSTRAINT fk_detalles_comprobante_tenant
        FOREIGN KEY (tenant_id, comprobante_id) REFERENCES dbo.comprobantes(tenant_id, id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'fk_detalles_producto_tenant')
    ALTER TABLE dbo.comprobantes_detalle ADD CONSTRAINT fk_detalles_producto_tenant
        FOREIGN KEY (tenant_id, producto_id) REFERENCES dbo.productos(tenant_id, id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'fk_comprobantes_lote_resumen_tenant')
    ALTER TABLE dbo.comprobantes ADD CONSTRAINT fk_comprobantes_lote_resumen_tenant
        FOREIGN KEY (tenant_id, lote_resumen_id) REFERENCES dbo.sunat_lotes(tenant_id, id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'fk_comprobantes_lote_baja_tenant')
    ALTER TABLE dbo.comprobantes ADD CONSTRAINT fk_comprobantes_lote_baja_tenant
        FOREIGN KEY (tenant_id, lote_baja_id) REFERENCES dbo.sunat_lotes(tenant_id, id);
