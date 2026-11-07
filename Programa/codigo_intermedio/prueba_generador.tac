DeclVal#char letrica
char1 = '5'
letrica = char1
data#int _arr_[20]
DeclVal#int _i_
int2 = 5
_i_ = int2
DeclVal#int arr[3]
int3 = 22
int4 = 10
int5 = 11
arr[0] = int3
arr[1] = int4
arr[2] = int5
goto void_principal

int_f:
int6 = 1
return int6
int_f_final

bool__funcionInt_:
DeclVal#bool result
bool7 = true
int8 = 12
int9 = 34
int10 = 35
int11 = int9 + int10
bool12 = int8 >= int11
bool13 = not bool12
bool14 = bool7 and bool13
result = bool14

_funcionInt__while_1_condicion:
bool15 = true
if bool15 goto _funcionInt__while_1_bloque
goto _funcionInt__while_1_end

_funcionInt__while_1_bloque:
int16 = _i_
write int16
goto _funcionInt__while_1_end
goto _funcionInt__while_1_cond

_funcionInt__while_1_end:
bool17 = true
return bool17
bool__funcionInt__final

bool__func2_:
param_bool _b1_
param_int _i1_
DeclVal#bool x
int18 = 22
int19 = 45
bool20 = int18 < int19
x = bool20
bool21 = x
if bool21 goto _func2__if_2_bloque1
goto _func2__if_2_sig1

_func2__if_2_bloque1:
data#int y

_func2__if_2_sig1:

_func2__if_2_end:
bool22 = x
return bool22
bool__func2__final

void_principal:
DeclVal#int var
int23 = 10
var = int23
int24 = 22
int25 = 45
bool26 = int24 < int25
int27 = var
int28 = 5
int29 = 6
int30 = int28 // int29
bool31 = int27 > int30
bool32 = bool26 and bool31
if bool32 goto principal_if_3_bloque1
goto principal_if_3_sig1

principal_if_3_bloque1:
data#int y
DeclVal#char ch33
char33 = 'a'
ch33 = char33
goto principal_if_3_final

principal_if_3_sig1:
bool34 = true
bool35 = false
bool36 = bool34 and bool35
bool37 = true
bool38 = bool36 or bool37
if bool38 goto principal_if_3_bloque2
goto principal_if_3_sig2

principal_if_3_bloque2:
data#int y
goto principal_if_3_final

principal_if_3_sig2:
data#int y
DeclVal#string str2
string39 = "sdff"
str2 = string39

principal_if_3_end:
data#int x
DeclVal#int edades[3]
int40 = 22
int41 = 10
int42 = 11
edades[0] = int40
edades[1] = int41
edades[2] = int42

principal_for_4_inicializacion:
int43 = 0
x = int43

principal_for_4_condicion:
goto principal_for_4_bloque

principal_for_4_inc:
int44 = x
int45 = int44 + 1
x = int45
goto principal_for_4_cond

principal_for_4_bloque:
string46 = "Hola mundo"
write string46
int47 = x
int48 = 10
bool49 = int47 > int48
if bool49 goto principal_if_5_bloque1
goto principal_if_5_sig1

principal_if_5_bloque1:
int50 = edades[2]
int51 = 1
int52 = int50 + int51
edades[2] = int52
goto principal_for_4_end

principal_if_5_sig1:

principal_if_5_end:
goto principal_for_4_inc

principal_for_4_end:
data#int filas[5]
read filas[0]
int53 = 3
int54 = _i_
int55 = int53 + int54
int56 = -1
int57 = 5
int58 = int56 + int57
_arr_[int55] = int58
bool59 = true
int60 = 5
param bool59
param int60
bool61 = call bool__func2_, 2
string62 = "fin del programa"
write string62
return
void_principal_final
