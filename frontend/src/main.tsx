import React from 'react'
import ReactDOM from 'react-dom/client'
import { SqlGeneratorApp } from './SqlGeneratorApp'
import './index.css'

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <SqlGeneratorApp />
  </React.StrictMode>,
)

