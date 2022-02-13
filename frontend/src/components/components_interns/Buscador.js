import React, { Component } from 'react';

import './Buscador.css';

class App extends Component {

  render() {
    return (
        <div class="p-3 mb-3">
            <h1 class="text-center buscarTitulo"> Buscar Restaurante</h1>
            <div class="text-center">
                <form class="d-flex w-50 justify-content-center mx-auto">
                    <select class="form-select mx-4 inputBuscador w-auto" aria-label="Default select example">
                        <option selected>Etiquetas</option>
                        <option value="1">One</option>
                        <option value="2">Two</option>
                        <option value="3">Three</option>
                    </select>

                    <select class="form-select mx-4 inputBuscador w-auto" aria-label="Default select example">
                        <option selected>Sitio</option>
                        <option value="1">One</option>
                        <option value="2">Two</option>
                        <option value="3">Three</option>
                    </select>

                    <select class="form-select mx-4 inputBuscador w-auto" aria-label="Default select example">
                        <option selected>Precio</option>
                        <option value="1">One</option>
                        <option value="2">Two</option>
                        <option value="3">Three</option>
                    </select>
                </form>
            </div>
        </div>
    );
  }
}

export default App;