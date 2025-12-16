export interface QueryDefinition {
  id: string
  name?: string
  description?: string
  tags?: string[]
  sqlFile: string
  parameters: ParameterDefinition[]
}

export interface ParameterDefinition {
  name: string
  type: string // 'text' | 'number' | 'integer' | 'date' | 'file'
  label?: string
  required: boolean
  isFile: boolean
}

export interface OpenApiSpec {
  openapi: string
  info: {
    title: string
    version: string
    description?: string
  }
  paths: {
    [path: string]: {
      post?: {
        summary?: string
        description?: string
        requestBody?: {
          content?: {
            'application/x-www-form-urlencoded'?: {
              schema?: OpenApiSchema
            }
            'multipart/form-data'?: {
              schema?: OpenApiSchema
            }
          }
        }
      }
    }
  }
}

export interface OpenApiSchema {
  type?: string
  properties?: {
    [key: string]: {
      type?: string
      format?: string
      description?: string
      example?: string
      enum?: string[]
      default?: string
    }
  }
  required?: string[]
}

