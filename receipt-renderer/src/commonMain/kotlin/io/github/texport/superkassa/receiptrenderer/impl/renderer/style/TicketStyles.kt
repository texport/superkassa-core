package io.github.texport.superkassa.receiptrenderer.impl.renderer.style

internal object TicketStyles {
    val TICKET_CSS = """
        .excise-stamps {
            font-size: 0.85em;
            color: #475569;
            color: var(--m3-on-surface-variant);
            margin-top: 4px;
            background: #f1f5f9;
            background: var(--m3-surface-variant);
            padding: 2px 6px;
            border-radius: 4px;
            display: inline-block;
            word-break: break-all;
        }
        .item-discount {
            font-size: 0.85em;
            color: #ef4444;
            color: var(--m3-error);
            margin-top: 4px;
            font-weight: 600;
        }
        .item-markup {
            font-size: 0.85em;
            color: #10b981;
            color: var(--m3-success);
            margin-top: 4px;
            font-weight: 600;
        }
        .item-vat {
            font-size: 0.85em;
            color: #475569;
            color: var(--m3-on-surface-variant);
            margin-top: 4px;
            font-weight: 600;
        }
        .items-table {
            width: 100%;
            border-collapse: collapse;
            table-layout: fixed;
        }
        .payments-table, .summary-table {
            width: 100%;
            border-collapse: collapse;
        }
        .items-table th {
            font-size: 0.8em;
            color: #475569;
            color: var(--m3-on-surface-variant);
            text-transform: uppercase;
            letter-spacing: 0.8px;
            border-bottom: 2px solid #e2e8f0;
            border-bottom: 2px solid var(--m3-outline-variant);
            padding: 8px 4px;
            text-align: left;
            font-weight: 700;
            word-wrap: break-word;
        }
        .items-table th.num { text-align: right; }
        .items-table td {
            padding: 8px 4px;
            border-bottom: 1px solid #e2e8f0;
            border-bottom: 1px solid var(--m3-outline-variant);
            vertical-align: top;
        }
        .items-table th:first-child, .items-table td:first-child { padding-left: 0; width: 34%; }
        .items-table th:nth-child(2), .items-table td:nth-child(2) { width: 22%; }
        .items-table th:nth-child(3), .items-table td:nth-child(3) { width: 22%; }
        .items-table th:last-child, .items-table td:last-child { padding-right: 0; width: 22%; }

        .tape-58mm .items-table th:first-child, .tape-58mm .items-table td:first-child { width: 30%; }
        .tape-58mm .items-table th:nth-child(2), .tape-58mm .items-table td:nth-child(2) { width: 22%; }
        .tape-58mm .items-table th:nth-child(3), .tape-58mm .items-table td:nth-child(3) { width: 24%; }
        .tape-58mm .items-table th:last-child, .tape-58mm .items-table td:last-child { width: 24%; }

        .payments-table td, .summary-table td {
            padding: 5px 0;
            vertical-align: middle;
            word-break: keep-all;
            word-wrap: normal;
        }
        .payments-table td:last-child, .summary-table td:last-child {
            text-align: right;
            white-space: nowrap;
        }
        .num { text-align: right; }
        td.num { white-space: nowrap; font-weight: 500; }
        .name { padding-right: 8px; font-weight: 600; }
        .summary-table .grand td {
            font-weight: 800;
            font-size: 1.25em;
            color: #6366f1;
            color: var(--m3-primary);
            border-top: 2px solid #6366f1;
            border-top: 2px solid var(--m3-primary);
            padding-top: 10px;
        }

        .receipt-link {
            margin-top: 16px;
            word-break: break-all;
            background-color: #f1f5f9;
            background-color: var(--m3-surface-variant);
            border: 1px solid #e2e8f0;
            border: 1px solid var(--m3-outline-variant);
            border-radius: 12px;
            padding: 10px 14px;
            font-size: 0.85em;
            color: #475569;
            color: var(--m3-on-surface-variant);
            text-align: center;
        }
        .receipt-link a {
            color: #6366f1;
            color: var(--m3-primary);
            text-decoration: none;
            font-weight: 600;
            display: block;
            margin-top: 4px;
        }
        .receipt-link a:hover { text-decoration: underline; }

        .qr { margin-top: 16px; text-align: center; }
        .qr img {
            width: 130px;
            height: 130px;
            border: 1px solid #e2e8f0;
            border: 1px solid var(--m3-outline-variant);
            border-radius: 16px;
            padding: 8px;
            background: #ffffff;
            box-shadow: 0 4px 12px rgba(99, 102, 241, 0.05);
            box-shadow: 0 4px 12px var(--m3-shadow);
        }
        .qr::after {
            content: "Scan to verify / Тексеру үшін сканерлеңіз";
            display: block;
            margin-top: 6px;
            font-size: 0.8em;
            color: #475569;
            color: var(--m3-on-surface-variant);
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }

        .item-row-card {
            background: #f1f5f9;
            background: var(--m3-surface-variant);
            border-radius: 12px;
            padding: 8px 12px;
            margin-bottom: 8px;
            border: 1px solid #e2e8f0;
            border: 1px solid var(--m3-outline-variant);
        }
        .item-row-table {
            width: 100%;
            border-collapse: collapse;
        }
        .item-row-table td { padding: 2px 0; border: none; }
        .item-name-cell {
            font-weight: 700;
            color: #0f172a;
            color: var(--m3-on-surface);
            word-break: break-word;
            overflow-wrap: anywhere;
            hyphens: auto;
            font-size: 1.05em;
        }
        .item-sum-cell {
            text-align: right;
            white-space: nowrap;
            font-weight: 700;
            color: #0f172a;
            color: var(--m3-on-surface);
            vertical-align: top;
        }
        .item-details-cell {
            font-size: 0.9em;
            color: #475569;
            color: var(--m3-on-surface-variant);
            line-height: 1.4;
        }

        .storno-item .item-name-cell { text-decoration: line-through; opacity: 0.6; }
        .storno-item .item-sum-cell { text-decoration: line-through; opacity: 0.6; }
        .storno-item .item-details-cell { opacity: 0.7; }
        .storno-badge {
            display: inline-block;
            background-color: #fee2e2;
            background-color: var(--m3-error-container);
            color: #991b1b;
            color: var(--m3-on-error-container);
            font-size: 0.75em;
            font-weight: 700;
            padding: 1px 5px;
            border-radius: 4px;
            margin-left: 6px;
            text-transform: uppercase;
            vertical-align: middle;
        }

        .parent-ticket-card {
            background-color: #fef3c7;
            background-color: var(--m3-warning-container);
            color: #92400e;
            color: var(--m3-on-warning-container);
            border: 1px solid #cbd5e1;
            border: 1px solid var(--m3-outline-variant);
            border-radius: 12px;
            padding: 8px 12px;
            margin-bottom: 8px;
        }
        .parent-ticket-table {
            width: 100%;
            border-collapse: collapse;
        }
        .parent-ticket-table td { padding: 2px 0; border: none; }
        .parent-ticket-table td:first-child { width: 55%; font-size: 0.9em; }
        .parent-ticket-table td:last-child { text-align: right; font-weight: 700; }

        .items-table td, .summary-table td, .parent-ticket-table td, .item-row-table td {
            word-break: keep-all;
            word-wrap: normal;
        }

        @media print {
            .item-row-card {
                background: transparent !important;
                border: none !important;
                border-radius: 0 !important;
                padding: 6px 0 !important;
                margin-bottom: 0 !important;
                border-bottom: 1px solid #000000 !important;
            }
            .parent-ticket-card {
                background: transparent !important;
                border: 1px solid #000000 !important;
                color: #000000 !important;
                border-radius: 0 !important;
                padding: 6px 0 !important;
                margin-bottom: 0 !important;
                border-bottom: 1px solid #000000 !important;
            }
            .receipt-link { border: 1px solid #000000; background-color: transparent; border-radius: 0; }
            .receipt-link a { color: #000000; }
            .qr img { border-radius: 0; border: 1px solid #000000; box-shadow: none; }
            .qr::after { display: none !important; }
        }
    """.trimIndent()
}
