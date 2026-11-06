*&---------------------------------------------------------------------*
*& Function Module Z_IF_LOG_READ
*&---------------------------------------------------------------------*
*& RFC enabled function module that reads the interface message log, so the
*& cockpit can run against a live system instead of the local model.
*&
*& CREATED WITH SE37 -> "Remote-Enabled" MUST BE TICKED.
*& Without that flag the function module is not registered for RFC and the
*& call fails with "not found / not remote enabled" - which appears in
*& SM58 on the calling system, not as a dump here.
*&
*& In a live landscape the message log is usually read from the RFC
*& statistics buffer (TRFC) and the IDoc log (EDIDC). This function
*& module reads a consolidated view of them and returns it as one
*& RFC table parameter, so the cockpit does not need to know where each
*& message type came from.
*&---------------------------------------------------------------------*

FUNCTION z_if_log_read.
*"*" Local Interface:
*"*" IMPORTING
*"*" VALUE(iv_interface) TYPE string OPTIONAL
*"*" VALUE(iv_hours) TYPE i DEFAULT 24
*"*" VALUE(iv_max_rows) TYPE i DEFAULT 1000
*"*" EXPORTING
*"*" VALUE(ev_total) TYPE i
*"*" VALUE(ev_message) TYPE string
*"*" TABLES
*"*" it_messages TYPE zif_log_msg_tab

  DATA: ls_message TYPE zif_log_msg,
        lv_cutoff   TYPE timestamp,
        lv_count    TYPE i.

  CLEAR: ev_total, ev_message, it_messages.

  lv_cutoff = sy-timESTAMP - ( iv_hours * 3600 ).

  " --- RFC traffic (TRFC) -----------------------------------------------
  SELECT trc_mandt trc_timeint trc_name trc_no trc_userid
         FROM trfc
    WHERE trc_timeint >= @lv_cutoff
      AND ( iv_interface IS INITIAL OR trc_name = @iv_interface )
    ORDER BY trc_timeint DESCENDING
    INTO TABLE @DATA(lt_rfc).

  LOOP AT lt_rfc INTO DATA(ls_rfc).
    IF lv_count >= iv_max_rows.
      EXIT.
    ENDIF.
    lv_count = lv_count + 1.
    APPEND VALUE #(
      msg_id      = |RFC{ lv_count }|
      iface       = ls_rfc-trc_name
      msg_type    = 'RFC'
      status      = COND #( WHEN ls_rfc-trc_error IS INITIAL THEN 'OK' ELSE 'ERROR' )
      err_code    = ls_rfc-trc_error
      duration_ms = ls_rfc-trc_timeint
      host        = sy-host
      corr_id     = ls_rfc-trc_no
      created_at  = ls_rfc-trc_timeint
    ) TO it_messages.
  ENDLOOP.

  " --- IDoc traffic (EDIDC) ----------------------------------------------
  SELECT edi_sprs edi_chnam edi_status edi_error edi_trid edi_datum
    FROM edidc
   WHERE edi_datum >= @lv_cutoff
     AND ( iv_interface IS INITIAL OR edi_trid = @iv_interface )
   ORDER BY edi_datum DESCENDING
   INTO TABLE @DATA(lt_idoc).

  LOOP AT lt_idoc INTO DATA(ls_idoc).
    IF lv_count >= iv_max_rows.
      EXIT.
    ENDIF.
    lv_count = lv_count + 1.
    APPEND VALUE #(
      msg_id      = |ID{ lv_count }|
      iface       = ls_idoc-edi_trid
      msg_type    = 'IDOC'
      status      = COND #( WHEN ls_idoc-edi_status = '03' THEN 'OK' ELSE 'ERROR' )
      err_code    = ls_idoc-edi_error
      duration_ms = 0
      host        = sy-host
      corr_id     = ls_idoc-edi_sprs
      created_at  = ls_idoc-edi_datum
    ) TO it_messages.
  ENDLOOP.

  ev_total = lv_count.

  IF ev_total = 0.
    ev_message = 'No interface traffic in the requested window.'.
  ELSE.
    ev_message = |{ ev_total } message(s) returned.|.
  ENDIF.

ENDFUNCTION.
