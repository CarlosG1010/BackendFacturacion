IF OBJECT_ID(N'dbo.tenants', N'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('dbo.tenants', 'estado_sunat') IS NULL
        ALTER TABLE dbo.tenants ADD estado_sunat VARCHAR(50) NULL;
    IF COL_LENGTH('dbo.tenants', 'condicion_sunat') IS NULL
        ALTER TABLE dbo.tenants ADD condicion_sunat VARCHAR(50) NULL;
    IF COL_LENGTH('dbo.tenants', 'logo_path') IS NULL
        ALTER TABLE dbo.tenants ADD logo_path VARCHAR(500) NULL;
END;

IF OBJECT_ID(N'dbo.comprobantes', N'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('dbo.comprobantes', 'monto_exonerado') IS NULL
        ALTER TABLE dbo.comprobantes ADD monto_exonerado DECIMAL(14,2) NOT NULL CONSTRAINT df_comprobantes_monto_exonerado DEFAULT 0;
    IF COL_LENGTH('dbo.comprobantes', 'monto_inafecto') IS NULL
        ALTER TABLE dbo.comprobantes ADD monto_inafecto DECIMAL(14,2) NOT NULL CONSTRAINT df_comprobantes_monto_inafecto DEFAULT 0;
    IF COL_LENGTH('dbo.comprobantes', 'monto_gratuito') IS NULL
        ALTER TABLE dbo.comprobantes ADD monto_gratuito DECIMAL(14,2) NOT NULL CONSTRAINT df_comprobantes_monto_gratuito DEFAULT 0;
    IF COL_LENGTH('dbo.comprobantes', 'moneda') IS NULL
        ALTER TABLE dbo.comprobantes ADD moneda CHAR(3) NOT NULL CONSTRAINT df_comprobantes_moneda DEFAULT 'PEN';
    IF COL_LENGTH('dbo.comprobantes', 'tipo_cambio') IS NULL
        ALTER TABLE dbo.comprobantes ADD tipo_cambio DECIMAL(14,6) NULL;
    IF COL_LENGTH('dbo.comprobantes', 'lote_resumen_id') IS NULL
        ALTER TABLE dbo.comprobantes ADD lote_resumen_id UNIQUEIDENTIFIER NULL;
    IF COL_LENGTH('dbo.comprobantes', 'lote_baja_id') IS NULL
        ALTER TABLE dbo.comprobantes ADD lote_baja_id UNIQUEIDENTIFIER NULL;
    IF COL_LENGTH('dbo.comprobantes', 'estado_anulacion') IS NULL
        ALTER TABLE dbo.comprobantes ADD estado_anulacion VARCHAR(30) NULL;
    IF COL_LENGTH('dbo.comprobantes', 'hash_tributario') IS NULL
        ALTER TABLE dbo.comprobantes ADD hash_tributario VARCHAR(128) NULL;
    IF COL_LENGTH('dbo.comprobantes', 'codigo_respuesta_sunat') IS NULL
        ALTER TABLE dbo.comprobantes ADD codigo_respuesta_sunat VARCHAR(20) NULL;
    IF COL_LENGTH('dbo.comprobantes', 'mensaje_respuesta_sunat') IS NULL
        ALTER TABLE dbo.comprobantes ADD mensaje_respuesta_sunat VARCHAR(2000) NULL;
    IF COL_LENGTH('dbo.comprobantes', 'xml_firmado_path') IS NULL
        ALTER TABLE dbo.comprobantes ADD xml_firmado_path VARCHAR(1000) NULL;
    IF COL_LENGTH('dbo.comprobantes', 'cdr_path') IS NULL
        ALTER TABLE dbo.comprobantes ADD cdr_path VARCHAR(1000) NULL;
    IF COL_LENGTH('dbo.comprobantes', 'ticket_sunat') IS NULL
        ALTER TABLE dbo.comprobantes ADD ticket_sunat VARCHAR(100) NULL;
    IF COL_LENGTH('dbo.comprobantes', 'fecha_envio_sunat') IS NULL
        ALTER TABLE dbo.comprobantes ADD fecha_envio_sunat DATETIME2 NULL;
END;

IF OBJECT_ID(N'dbo.comprobantes', N'U') IS NOT NULL
   AND COL_LENGTH('dbo.comprobantes', 'monto_gravado') IS NULL
BEGIN
    THROW 51000, 'El esquema base de comprobantes no existe o no es compatible; aplique primero la migracion inicial del sistema.', 1;
END;

IF OBJECT_ID(N'dbo.comprobantes_detalle', N'U') IS NOT NULL
BEGIN
    IF COL_LENGTH('dbo.comprobantes_detalle', 'tipo_afectacion_igv') IS NULL
        ALTER TABLE dbo.comprobantes_detalle ADD tipo_afectacion_igv VARCHAR(2) NOT NULL CONSTRAINT df_detalle_afectacion DEFAULT '10';
    IF COL_LENGTH('dbo.comprobantes_detalle', 'es_gratuito') IS NULL
        ALTER TABLE dbo.comprobantes_detalle ADD es_gratuito BIT NOT NULL CONSTRAINT df_detalle_gratuito DEFAULT 0;
END;

IF OBJECT_ID(N'dbo.sunat_lotes', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.sunat_lotes (
        id UNIQUEIDENTIFIER NOT NULL CONSTRAINT pk_sunat_lotes PRIMARY KEY,
        tenant_id UNIQUEIDENTIFIER NOT NULL,
        tipo_lote VARCHAR(20) NOT NULL,
        fecha_documentos DATE NOT NULL,
        correlativo INT NOT NULL,
        documentos_json NVARCHAR(MAX) NOT NULL,
        nombre_archivo VARCHAR(120) NOT NULL,
        estado VARCHAR(30) NOT NULL,
        ticket_sunat VARCHAR(100) NULL,
        codigo_respuesta VARCHAR(20) NULL,
        mensaje_respuesta VARCHAR(2000) NULL,
        xml_firmado_path VARCHAR(1000) NULL,
        cdr_path VARCHAR(1000) NULL,
        creado_en DATETIME2 NOT NULL CONSTRAINT df_sunat_lotes_creado DEFAULT SYSUTCDATETIME(),
        fecha_respuesta DATETIME2 NULL
    );
    CREATE INDEX idx_sunat_lotes_tenant_estado
        ON dbo.sunat_lotes(tenant_id, estado, creado_en);
END;

IF OBJECT_ID(N'dbo.sunat_lotes', N'U') IS NOT NULL
   AND COL_LENGTH('dbo.sunat_lotes', 'correlativo') IS NULL
BEGIN
    ALTER TABLE dbo.sunat_lotes ADD correlativo INT NOT NULL
        CONSTRAINT df_sunat_lotes_correlativo DEFAULT 1;
END;

IF OBJECT_ID(N'dbo.sunat_lotes', N'U') IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'uk_sunat_lotes_correlativo'
                   AND object_id = OBJECT_ID(N'dbo.sunat_lotes'))
BEGIN
    CREATE UNIQUE INDEX uk_sunat_lotes_correlativo
        ON dbo.sunat_lotes(tenant_id, tipo_lote, fecha_documentos, correlativo);
END;
