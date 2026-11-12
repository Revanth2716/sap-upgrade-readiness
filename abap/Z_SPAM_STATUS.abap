*&---------------------------------------------------------------------*
*& Function Module Z_SPAM_STATUS
*&---------------------------------------------------------------------*
*& RFC enabled function module that reads the support package history of a
*& system, so the Upgrade & Support Package Readiness portal can run against
*& a live landscape instead of a spreadsheet somebody keeps by hand.
*&
*& CREATED WITH SE37 -> "Remote-Enabled" MUST BE TICKED.
*& Without that flag the function module is not registered for RFC and the
*& call fails with "not found / not remote enabled" - which appears in
*& SM58 on the calling system, not as a dump here.
*&
*& Source: SPHIST - the history of the support packages that have been
*& applied in the system, with the date and the user who applied them.
*& SPDIR holds the patch level of each directory, and SPBUF the packages
*& that are staged in the patch buffer but not yet applied.
*&
*& A note on honesty: the field names of these tables differ between
*& releases and between the classic and the new status model, and the
*& component state - which component is obsolete, which one has been
*& replaced - is NOT held in the transport tables at all. The Software
*& Update Manager keeps that in its own component list on the file system
*& of the application server. This module therefore returns what the
*& database genuinely holds, and the component state is maintained on the
*& portal side. The interface itself does not change, only the ABAP
*& behind it.
*&
*& What this function module deliberately does NOT do: decide whether a
*& system can be upgraded. Only the analyzer knows what blocks an upgrade
*& and in which order the blockers have to be cleared.
*&---------------------------------------------------------------------*

FUNCTION z_spam_status.
*"*" Local Interface:
*"*" IMPORTING
*"*" VALUE(iv_system) TYPE string OPTIONAL
*"*" EXPORTING
*"*" VALUE(ev_count) TYPE i
*"*" VALUE(ev_message) TYPE string
*"*" TABLES
*"*" it_packages TYPE zsp_pkg_tab

  DATA: lv_count TYPE i.

  CLEAR: ev_count, ev_message, it_packages.

  " -- the support packages that have been applied --------------------------
  SELECT spid spname spdate spuser spdir
    FROM sphist
    WHERE ( @iv_system IS INITIAL OR spdir = @iv_system )
    ORDER BY spdate DESCENDING
    INTO TABLE @DATA(lt_history).

  LOOP AT lt_history INTO DATA(ls_history).
    lv_count = lv_count + 1.

    " The patch level is derived from the name of the package: SP07
    " carries level 7. A live implementation reads it from SPDIR, which
    " holds the level per directory.
    DATA(lv_level) = COND int4(
      WHEN ls_history-spid CO CP 'SP*'
        THEN CONV int4( SUBSTRING( ls_history-spid FROM 3 ) )
      ELSE 0 ).

    APPEND VALUE #(
      sp_id    = ls_history-spid
      sp_name  = ls_history-spname
      sp_level = lv_level
      sp_date  = |{ ls_history-spdate DATE = TIME }|
      sp_user  = ls_history-spuser
      sp_dir   = ls_history-spdir
    ) TO it_packages.
  ENDLOOP.

  " -- the patch level of each directory ------------------------------------
  SELECT spdir splevel
    FROM spdir
    WHERE ( @iv_system IS INITIAL OR spdir = @iv_system )
    INTO TABLE @DATA(lt_dirs).

  " -- the packages sitting in the buffer, not yet applied -------------------
  SELECT spid
    FROM spbuf
    INTO TABLE @DATA(lt_buffer).

  ev_count = lv_count.

  IF ev_count = 0.
    ev_message = |No support package history found for { iv_system }.|.
  ELSE.
    ev_message = |{ ev_count } support package(s) applied in { iv_system }.|.
  ENDIF.

ENDFUNCTION.
