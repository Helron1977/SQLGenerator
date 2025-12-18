/**
 * Types TypeScript correspondant aux modèles Java du backend
 */

export interface ParameterDefinition {
  name: string;
  type: 'text' | 'number' | 'date' | 'boolean' | 'file';
  label: string;
  required: boolean;
  description?: string;
  example?: string;
}

export interface FormField {
  name: string;
  type: string;
  label: string;
  required: boolean;
  description?: string;
  example?: string;
  technical?: boolean;
}

export interface FormSchema {
  templateId: string;
  name: string;
  description?: string;
  tags?: string[];
  hasInParameter: boolean;
  modes: string[];
  unitFields: FormField[];
  massFields?: FormField[] | null;
}

export interface RequestBodySchema {
  templateId: string;
  mode: 'unitaire' | 'masse';
  body: Record<string, unknown>;
}

