package io.github.texport.superkassa.receiptrenderer.impl.renderer.style

internal object MetaDocumentStyles {
    val META_DOCUMENT_CSS = """
        .meta-table {
            width: 100%;
            border-collapse: collapse;
        }
        .meta-table td {
            padding: 4px 0;
            vertical-align: middle;
            word-break: keep-all;
            word-wrap: normal;
        }
        .meta-table td:first-child {
            color: #475569;
            color: var(--m3-on-surface-variant);
            font-weight: 500;
        }
        .meta-table td:last-child {
            text-align: right;
            font-weight: 600;
            color: #0f172a;
            color: var(--m3-on-surface);
            white-space: nowrap;
        }

        .status-item {
            text-align: center;
        }
        .status-item-label {
            font-size: 0.9em;
            color: #475569;
            color: var(--m3-on-surface-variant);
            margin-bottom: 6px;
            font-weight: 600;
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }
        .status-item-badge {
            display: flex;
            justify-content: center;
        }

        .tax-row-card {
            display: block;
            margin-left: 0;
            margin-right: 0;
            padding-top: 12px;
            padding-bottom: 8px;
            padding-left: 12px;
            padding-right: 12px;
            background: #ffffff;
            background: var(--m3-surface);
            border: 1px solid #e2e8f0;
            border: 1px solid var(--m3-outline-variant);
            border-radius: 12px;
            margin-top: 14px;
            margin-bottom: 8px;
        }
        .tax-row-card.highlighted {
            border: 2px solid #6366f1;
            border: 2px solid var(--m3-primary);
            background: #f8fafc;
            background: var(--m3-surface-variant);
        }
        .tax-row-card.dashed { border-style: dashed; }
        .section-card {
            display: block;
            margin-left: 0;
            margin-right: 0;
            padding-top: 12px;
            padding-bottom: 8px;
            padding-left: 12px;
            padding-right: 12px;
            background: #ffffff;
            background: var(--m3-surface);
            border: 1px solid #e2e8f0;
            border: 1px solid var(--m3-outline-variant);
            border-radius: 8px;
            margin-top: 16px;
            margin-bottom: 12px;
        }
        .card-label {
            margin-left: 10px;
            background: #ffffff;
            background: var(--m3-surface);
            padding: 0 6px;
            font-size: 0.8em;
            font-weight: bold;
            color: #475569;
            color: var(--m3-on-surface-variant);
            text-transform: uppercase;
            letter-spacing: 0.5px;
            width: auto;
        }
        .tax-row-table {
            width: 100%;
            border-collapse: collapse;
        }
        .tax-row-table td { 
            padding: 2px 0; 
            border: none;
            word-break: keep-all;
            word-wrap: normal;
        }
        .tax-rate-cell { color: #0f172a; color: var(--m3-on-surface); }
        .tax-sum-cell {
            text-align: right;
            white-space: nowrap;
            color: #6366f1;
            color: var(--m3-primary);
        }
        .tax-details-cell {
            font-size: 0.9em;
            color: #475569;
            color: var(--m3-on-surface-variant);
        }
        .taxes-list { width: 100%; }

        @media print {
            .tax-row-card, .section-card {
                background: transparent !important;
                border: none !important;
                border-radius: 0 !important;
                padding: 6px 0 !important;
                margin-top: 10px !important;
                margin-bottom: 0 !important;
                border-bottom: 1px solid #000000 !important;
            }
            .card-label {
                position: static !important;
                margin-top: 0 !important;
                background: transparent !important;
                padding: 0 !important;
                font-size: 0.95em !important;
                color: #000000 !important;
                display: block !important;
                margin-bottom: 4px !important;
            }
        }
    """.trimIndent()
}
