<?php
header('Access-Control-Allow-Origin: *');
header("Access-Control-Allow-Headers: X-API-KEY, Origin, X-Requested-With, Content-Type, Accept, Access-Control-Request-Method");
header("Access-Control-Allow-Methods: GET, POST, OPTIONS, PUT, DELETE");
header("Allow: GET, POST, OPTIONS, PUT, DELETE");

use App\Http\Controllers\HorarioController;
use App\Http\Controllers\ReservasController;
use App\Http\Controllers\UseracountController;
use Illuminate\Support\Facades\Route;
use App\Http\Controllers\CartaController;
use App\Http\Controllers\RestaurantController;
use App\Models\Carta;

/*
|--------------------------------------------------------------------------
| Web Routes
|--------------------------------------------------------------------------
|
| Here is where you can register web routes for your application. These
| routes are loaded by the RouteServiceProvider within a group which
| contains the "web" middleware group. Now create something great!
|
*/

Route::get('/', function () {
    return view('welcome');
});

Route::get('card/{id}', [CartaController::class, 'showCard']);

Route::get('/restaurant/{id}',[RestaurantController::class, 'show']);
Route::get('/restaurants',[RestaurantController::class, 'showRestaurants']);
Route::get('/bestrestaurants',[RestaurantController::class, 'showRestaurantsComplet']);

Route::get('/user/{id}',[UseracountController::class, 'show']);
Route::get('/users',[UseracountController::class, 'showUsers']);
Route::get('/adminUsers',[UseracountController::class, 'showUserWhereIsAdmin']);

Route::get('/reserva/{id}',[ReservasController::class, 'show']);
Route::post('/reserva',[ReservasController::class, 'reservasRestaurant']);

Route::get('/horario/{id}',[HorarioController::class, 'show']);
