*&---------------------------------------------------------------------*
*& DDIC types for the Upgrade & Support Package Readiness RFC interface
*&---------------------------------------------------------------------*
*& Create these in SE11. Flat structures, so they can be used as RFC
*& table parameters without any conversion rules.
*&---------------------------------------------------------------------*

"! <p class="text">One applied support package - the shape the analyzer
"! expects from a live system.</p>
TYPES : BEGIN OF zsp_pkg,
         sp_id     TYPE string,   " Support package, e.g. SP07
         sp_name   TYPE string,   " Descriptive text
         sp_level  TYPE int4,     " Patch level the package carries
         sp_date   TYPE string,   " Applied, YYYYMMDDHHMMSS
         sp_user   TYPE string,   " Who applied it
         sp_dir    TYPE string,   " The directory it was applied from
       END OF zsp_pkg.

TYPES zsp_pkg_tab TYPE STANDARD TABLE OF zsp_pkg.
