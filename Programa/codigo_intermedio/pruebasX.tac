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
DeclVal#float pi
float6 = 3.14
pi = float6
DeclVal#float notas[3]
float7 = 9.5
float8 = 8.0
float9 = 7.25
notas[0] = float7
notas[1] = float8
notas[2] = float9
DeclVal#bool activo
bool10 = true
activo = bool10
DeclVal#string saludo
string11 = "hola"
saludo = string11
goto void_principal

int_f:
int12 = 1
return int12
int_f_final

int_suma:
param_int a
param_int b
DeclVal#int r
int13 = a
int14 = b
int15 = int13 + int14
int16 = 2
int17 = int15 * int16
r = int17
int18 = r
int19 = a
int20 = b
int21 = int19 // int20
int22 = int18 - int21
r = int22
int23 = r
int24 = 3
int25 = int23 mod int24
r = int25
int26 = r
int27 = 2
int28 = 3
int29 = int27 pot int28
int30 = int26 + int29
r = int30
int31 = r
return int31
int_suma_final

float_promedio:
param_float x
param_float y
DeclVal#float total
float32 = x
float33 = y
float34 = float32 + float33
total = float34
float35 = total
float36 = 2.0
float37 = float35 / float36
return float37
float_promedio_final

char_inicial:
param_char c
param_bool mayus
bool38 = mayus
if bool38 goto inicial_if_1_bloque1
goto inicial_if_1_sig1

inicial_if_1_bloque1:
char39 = 'A'
return char39

inicial_if_1_sig1:

inicial_if_1_final:
char40 = c
return char40
char_inicial_final

bool_distintos:
param_int a
param_int b
int41 = a
int42 = b
bool43 = int41 != int42
return bool43
bool_distintos_final

bool__funcionInt_:
DeclVal#bool result
bool44 = true
int45 = 12
int46 = 34
int47 = 35
int48 = int46 + int47
bool49 = int45 >= int48
bool50 = not bool49
bool51 = bool44 and bool50
result = bool51

_funcionInt__while_2_condicion:
bool52 = true
if bool52 goto _funcionInt__while_2_bloque
goto _funcionInt__while_2_final

_funcionInt__while_2_bloque:
int53 = _i_
write int53
goto _funcionInt__while_2_final
goto _funcionInt__while_2_condicion

_funcionInt__while_2_final:
bool54 = true
return bool54
bool__funcionInt__final

void_principal:
DeclVal#int var
int55 = 10
var = int55
int56 = 22
int57 = 45
bool58 = int56 < int57
int59 = var
int60 = 5
int61 = 6
int62 = int60 // int61
bool63 = int59 > int62
bool64 = bool58 and bool63
if bool64 goto principal_if_3_bloque1
goto principal_if_3_sig1

principal_if_3_bloque1:
data#int y
DeclVal#char ch33
char65 = 'a'
ch33 = char65
goto principal_if_3_final

principal_if_3_sig1:
bool66 = true
bool67 = false
bool68 = bool66 and bool67
bool69 = true
bool70 = bool68 or bool69
if bool70 goto principal_if_3_bloque2
goto principal_if_3_sig2

principal_if_3_bloque2:
data#int y
goto principal_if_3_final

principal_if_3_sig2:
data#int y
DeclVal#string str2
string71 = "sdff"
str2 = string71

principal_if_3_final:
data#int x
DeclVal#int edades[3]
int72 = 22
int73 = 10
int74 = 11
edades[0] = int72
edades[1] = int73
edades[2] = int74

principal_for_4_inicializacion:
int75 = 0
x = int75

principal_for_4_condicion:
goto principal_for_4_bloque

principal_for_4_inc:
int76 = x
int77 = int76 + 1
x = int77
goto principal_for_4_condicion

principal_for_4_bloque:
string78 = "Hola mundo"
write string78
int79 = x
int80 = 10
bool81 = int79 > int80
if bool81 goto principal_if_5_bloque1
goto principal_if_5_sig1

principal_if_5_bloque1:
int82 = edades[2]
int83 = 1
int84 = int82 + int83
edades[2] = int84
goto principal_for_4_final

principal_if_5_sig1:

principal_if_5_final:
goto principal_for_4_inc

principal_for_4_final:
DeclVal#int uno
int85 = call int_f, 0
uno = int85
DeclVal#int s
int86 = 3
int87 = 4
param int86
param int87
int88 = call int_suma, 2
s = int88
int89 = _i_
int90 = 2
param int89
param int90
int91 = call int_suma, 2
DeclVal#float prom
float92 = 7.5
float93 = pi
param float92
param float93
float94 = call float_promedio, 2
prom = float94
DeclVal#char c
char95 = letrica
bool96 = activo
param char95
param bool96
char97 = call char_inicial, 2
c = char97
DeclVal#bool dif
int98 = s
int99 = 7
param int98
param int99
bool100 = call bool_distintos, 2
dif = bool100
DeclVal#bool resultadoBool
bool101 = call bool__funcionInt_, 0
resultadoBool = bool101
read var
read c
read _arr_[0]
int102 = s
write int102
float103 = prom
write float103
string104 = saludo
write string104
int105 = -5
write int105
DeclVal#int contador
int106 = 3
contador = int106

principal_while_6_condicion:
int107 = contador
int108 = 0
bool109 = int107 > int108
if bool109 goto principal_while_6_bloque
goto principal_while_6_final

principal_while_6_bloque:
int110 = contador
int111 = 3
bool112 = int110 == int111
if bool112 goto principal_if_7_bloque1
goto principal_if_7_sig1

principal_if_7_bloque1:
string113 = "tres"
write string113
goto principal_if_7_final

principal_if_7_sig1:
int114 = contador
int115 = 2
bool116 = int114 == int115
if bool116 goto principal_if_7_bloque2
goto principal_if_7_sig2

principal_if_7_bloque2:
string117 = "dos"
write string117
goto principal_if_7_final

principal_if_7_sig2:
int118 = contador
int119 = 1
bool120 = int118 <= int119
if bool120 goto principal_if_7_bloque3
goto principal_if_7_sig3

principal_if_7_bloque3:
string121 = "uno"
write string121

principal_if_7_sig3:

principal_if_7_final:
int122 = contador
int123 = 1
int124 = int122 - int123
contador = int124
goto principal_while_6_condicion

principal_while_6_final:
char125 = c
char126 = 'A'
bool127 = char125 == char126
bool128 = not bool127
if bool128 goto principal_if_8_bloque1
goto principal_if_8_sig1

principal_if_8_bloque1:
string129 = "no es A"
write string129

principal_if_8_sig1:

principal_if_8_final:

principal_for_9_inicializacion:
DeclVal#int i
int130 = 0
i = int130

principal_for_9_condicion:
int131 = i
int132 = 3
bool133 = int131 < int132
if bool133 goto principal_for_9_bloque
goto principal_for_9_final

principal_for_9_inc:
int134 = i
int135 = int134 + 1
i = int135
goto principal_for_9_condicion

principal_for_9_bloque:
int136 = arr[i]
int137 = 2
int138 = int136 * int137
arr[i] = int138
float139 = notas[i]
float140 = 2.0
float141 = float139 / float140
notas[i] = float141
int142 = i
int143 = 1
int144 = int142 + int143
int145 = arr[i]
_arr_[int144] = int145
goto principal_for_9_inc

principal_for_9_final:
data#int j

principal_for_10_inicializacion:
int146 = 10
j = int146

principal_for_10_condicion:
int147 = j
int148 = 0
bool149 = int147 >= int148
if bool149 goto principal_for_10_bloque
goto principal_for_10_final

principal_for_10_inc:
int150 = j
int151 = int150 - 1
j = int151
goto principal_for_10_condicion

principal_for_10_bloque:
int152 = j
int153 = 5
bool154 = int152 < int153
bool155 = activo
bool156 = bool154 or bool155
if bool156 goto principal_if_11_bloque1
goto principal_if_11_sig1

principal_if_11_bloque1:

principal_while_12_condicion:
int157 = j
int158 = 2
bool159 = int157 > int158
if bool159 goto principal_while_12_bloque
goto principal_while_12_final

principal_while_12_bloque:
int160 = j
int161 = 1
int162 = int160 - int161
j = int162
goto principal_while_12_final
goto principal_while_12_condicion

principal_while_12_final:
goto principal_for_10_final

principal_if_11_sig1:

principal_if_11_final:
goto principal_for_10_inc

principal_for_10_final:
return
void_principal_final
