<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Database\Eloquent\Model;

class Localidad extends Model
{
    use HasFactory;

    protected $table = "localidad";
    public $timestamps = false;
    protected $primaryKey = 'id_localidad';
}
