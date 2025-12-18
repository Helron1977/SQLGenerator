/**
 * Service API pour communiquer avec le backend SQL Generator
 * 
 * Toutes les URLs sont relatives pour faciliter l'intégration.
 * Le projet parent peut configurer un proxy ou une base URL.
 * 
 * Configuration via variable d'environnement :
 * - VITE_API_BASE_URL : URL de base de l'API (défaut: '/api')
 */

import { FormSchema, RequestBodySchema, ParameterDefinition, FormField } from "../types/api";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

/**
 * Récupère la liste de tous les schémas de formulaires disponibles
 */
export async function fetchAllFormSchemas(): Promise<FormSchema[]> {
  const response = await fetch(`${API_BASE_URL}/forms`);
  if (!response.ok) {
    throw new Error(`Erreur lors de la récupération des formulaires: ${response.statusText}`);
  }
  return response.json();
}

/**
 * Récupère le schéma d'un formulaire spécifique
 */
export async function fetchFormSchema(templateId: string): Promise<FormSchema> {
  const response = await fetch(`${API_BASE_URL}/forms/${templateId}`);
  if (!response.ok) {
    if (response.status === 404) {
      throw new Error(`Formulaire non trouvé: ${templateId}`);
    }
    throw new Error(`Erreur lors de la récupération du formulaire: ${response.statusText}`);
  }
  return response.json();
}

/**
 * Récupère le JSON de test pour un formulaire (mode unitaire)
 */
export async function fetchRequestBodySchema(templateId: string): Promise<RequestBodySchema> {
  const response = await fetch(`${API_BASE_URL}/forms/${templateId}/request-body`);
  if (!response.ok) {
    throw new Error(`Erreur lors de la récupération du schéma: ${response.statusText}`);
  }
  return response.json();
}

/**
 * Génère un script SQL en mode unitaire
 */
export async function generateScriptUnitaire(
  templateId: string,
  params: Record<string, string | File>
): Promise<Blob> {
  const formData = new FormData();
  
  // Ajouter les paramètres texte
  Object.entries(params).forEach(([key, value]) => {
    if (value instanceof File) {
      formData.append(key, value);
    } else {
      formData.append(key, value);
    }
  });

  const response = await fetch(`${API_BASE_URL}/scripts/${templateId}`, {
    method: 'POST',
    body: formData
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(`Erreur lors de la génération: ${errorText}`);
  }

  return response.blob();
}

/**
 * Génère des scripts SQL en mode masse
 */
export async function generateScriptMasse(
  templateId: string,
  csvFile: File
): Promise<Blob> {
  const formData = new FormData();
  formData.append('masseFile', csvFile);

  const response = await fetch(`${API_BASE_URL}/scripts/${templateId}/masse`, {
    method: 'POST',
    body: formData
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(`Erreur lors de la génération en masse: ${errorText}`);
  }

  return response.blob();
}

// Réexport des types pour faciliter l'utilisation
export type { FormSchema, RequestBodySchema, ParameterDefinition, FormField };

