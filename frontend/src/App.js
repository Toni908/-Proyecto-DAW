import React, { Component } from 'react';
import { BrowserRouter as Router, Routes, Route } from "react-router-dom";

import Home from "./Home";
import BuscadorAvanzado from "./BuscadorAvanzado";
import AboutUs from "./AboutUs";
import Header from "./components/Header";
import Footer from "./components/Footer";

import './App.css';
import Restaurant from "./components/paginas/Restaurant";

class App extends Component {

  render() {
    return (
      <Router>
        <div className="d-flex flex-column min-vh-100 App">

          <Header />

          <div className="content">
            <Routes>
              <Route path="/" element={<Home />} />
              <Route path="/search" element={<BuscadorAvanzado />} />
              <Route path="/us" element={<AboutUs />} />
              <Route path="/carta/:id" element={<AboutUs />} />
              <Route path="/comentario/:id" element={<AboutUs />} />
              <Route path="/restaurante/:id" element={<AboutUs />} />
              <Route path="/reserva/:id" element={<AboutUs />} />
              <Route path="/restaurant/:id" element={<Restaurant />} />
            </Routes>
          </div>
          
          <Footer />

        </div>
      </Router>
    );
  }
}

export default App;