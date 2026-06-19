package kz.mybrain.superkassa.core.data.receipt

object ReceiptHtmlStyles {
    const val CSS = """
                @page { size: A4; margin: 10mm; }
                * { box-sizing: border-box; }
                body {
                    margin: 0;
                    padding: 20px 8px;
                    color: #2d3748;
                    background-color: #f7fafc;
                    font-family: "Inter", "DejaVu Sans Mono", "Courier New", monospace;
                    font-size: 11px;
                    line-height: 1.4;
                    -webkit-font-smoothing: antialiased;
                }
                .receipt {
                    max-width: 85mm;
                    margin: 0 auto;
                    background: #ffffff;
                    border: 1px solid #e2e8f0;
                    border-radius: 8px;
                    padding: 20px;
                    box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05), 0 2px 4px -1px rgba(0,0,0,0.03);
                }
                .center { text-align: center; }
                .muted { color: #718096; font-size: 10px; margin-top: 4px; }
                .rule {
                    border-top: 1px dashed #cbd5e0;
                    margin: 12px 0;
                }
                .brand-header {
                    margin-bottom: 14px;
                }
                .brand-logo {
                    font-size: 16px;
                    font-weight: 800;
                    letter-spacing: 1px;
                    color: #1a202c;
                    margin-bottom: 4px;
                }
                .org-title {
                    font-size: 11px;
                    font-weight: 700;
                    margin-bottom: 2px;
                }
                .org-bin, .org-address {
                    font-size: 10px;
                    color: #4a5568;
                    margin-bottom: 2px;
                }
                .excise-stamps {
                    font-size: 8px;
                    color: #718096;
                    margin-top: 2px;
                    font-style: italic;
                    word-break: break-all;
                }
                .doc-title {
                    font-size: 12px;
                    font-weight: 700;
                    color: #2b6cb0;
                    text-transform: uppercase;
                    margin-top: 6px;
                }
                .meta-table, .items-table, .payments-table, .summary-table, .tax-table {
                    width: 100%;
                    border-collapse: collapse;
                }
                .meta-table td {
                    padding: 3px 0;
                    vertical-align: middle;
                }
                .meta-table td:first-child {
                    width: 55%;
                    color: #718096;
                }
                .meta-table td:last-child {
                    text-align: right;
                    font-weight: 600;
                    color: #2d3748;
                }
                .items-table th, .tax-table th {
                    font-size: 9px;
                    color: #718096;
                    text-transform: uppercase;
                    letter-spacing: 0.5px;
                    border-bottom: 2px solid #e2e8f0;
                    padding: 6px 0;
                    text-align: left;
                }
                .items-table th.num, .tax-table th.num {
                    text-align: right;
                }
                .items-table td, .tax-table td {
                    padding: 6px 0;
                    border-bottom: 1px solid #edf2f7;
                    vertical-align: top;
                }
                .payments-table td, .summary-table td {
                    padding: 4px 0;
                    vertical-align: middle;
                }
                .num {
                    text-align: right;
                    white-space: nowrap;
                }
                .name {
                    padding-right: 8px;
                    word-break: break-word;
                }
                .summary-table .grand td {
                    font-weight: 700;
                    font-size: 13px;
                    color: #1a202c;
                    border-top: 2px solid #2d3748;
                    padding-top: 6px;
                }
                .section-title {
                    font-size: 10px;
                    font-weight: 700;
                    text-transform: uppercase;
                    color: #4a5568;
                    margin-bottom: 6px;
                }
                .footer {
                    margin-top: 10px;
                    font-size: 10px;
                    color: #4a5568;
                }
                .footer-item {
                    margin-bottom: 4px;
                }
                .receipt-link {
                    margin-top: 10px;
                    word-break: break-all;
                    background-color: #f7fafc;
                    border: 1px solid #e2e8f0;
                    border-radius: 4px;
                    padding: 6px;
                    font-size: 9px;
                }
                .receipt-link a {
                    color: #3182ce;
                    text-decoration: none;
                    font-weight: 600;
                }
                .receipt-link a:hover {
                    text-decoration: underline;
                }
                .qr {
                    margin-top: 12px;
                    text-align: center;
                }
                .qr img {
                    width: 120px;
                    height: 120px;
                    border: 1px solid #e2e8f0;
                    border-radius: 6px;
                    padding: 6px;
                    background: #ffffff;
                }
                .badge {
                    display: inline-block;
                    padding: 2px 6px;
                    font-size: 9px;
                    font-weight: 700;
                    border-radius: 4px;
                    text-transform: uppercase;
                    letter-spacing: 0.3px;
                }
                .badge-success {
                    background-color: #c6f6d5;
                    color: #22543d;
                }
                .badge-warning {
                    background-color: #fefcbf;
                    color: #744210;
                }
                @media print {
                    body { background-color: #ffffff; padding: 0; color: #000000; }
                    .receipt { border: 0; max-width: none; padding: 0; box-shadow: none; }
                    .badge { background-color: transparent !important; color: #000000 !important; border: 1px solid #000000; padding: 1px 4px; }
                    .receipt-link { border: 1px solid #000000; background-color: transparent; }
                    .receipt-link a { color: #000000; }
                }
    """
}
