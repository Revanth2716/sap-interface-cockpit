*&---------------------------------------------------------------------*
*& DDIC types for the Interface Cockpit RFC interface
*&---------------------------------------------------------------------*
*& Create these in SE11. Flat structures, so they can be used as RFC
*& table parameters without any conversion rules.
*&---------------------------------------------------------------------*

"! <p class="text">One interface message - an RFC, IDoc or BAPI call
"! read from the transport statistics or the IDoc log.</p>
TYPES : BEGIN OF zif_log_msg,
         msg_id      TYPE string,   " Message identifier
         iface       TYPE string,   " Interface name
         msg_type    TYPE string,   " RFC / IDOC / BAPI
         status      TYPE string,   " OK / ERROR / WARNING
         err_code    TYPE string,   " Error code, if any
         duration_ms TYPE int4,     " Duration in milliseconds
         host        TYPE string,   " Host that handled the message
         corr_id     TYPE string,   " Correlation id
         created_at  TYPE timestamp," Message time
       END OF zif_log_msg.

TYPES zif_log_msg_tab TYPE STANDARD TABLE OF zif_log_msg.
