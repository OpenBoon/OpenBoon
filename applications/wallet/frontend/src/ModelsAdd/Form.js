import { useReducer } from 'react'
import { useRouter } from 'next/router'
import useSWR from 'swr'

import { spacing } from '../Styles'

import Form from '../Form'
import FlashMessageErrors from '../FlashMessage/Errors'
import SectionTitle from '../SectionTitle'
import Input, { VARIANTS as INPUT_VARIANTS } from '../Input'
import Textarea, { VARIANTS as TEXTAREA_VARIANTS } from '../Textarea'
import Radio from '../Radio'
import ButtonGroup from '../Button/Group'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'

import { onSubmit, slugify } from './helpers'

const PRE_TRAINED_MODEL_TYPE = 'PYTORCH_MODEL_ARCHIVE'

const SOURCES = [
  { value: 'CREATE', label: 'Train in Boon AI' },
  // { value: 'UPLOAD', label: 'Upload Pre-Trained Model' },
]

const INITIAL_STATE = {
  name: '',
  description: '',
  source: SOURCES[0].value,
  type: '',
  isLoading: false,
  errors: {},
}

const reducer = (state, action) => ({ ...state, ...action })

const ModelsAddForm = () => {
  const {
    query: { projectId },
  } = useRouter()

  const {
    data: { results: modelTypes },
  } = useSWR(`/api/v1/projects/${projectId}/models/model_types/`)

  const [state, dispatch] = useReducer(reducer, INITIAL_STATE)

  return (
    <Form>
      <FlashMessageErrors
        errors={state.errors}
        styles={{ marginTop: -spacing.base, paddingBottom: spacing.comfy }}
      />

      <SectionTitle>Model Name &amp; Description</SectionTitle>

      <Input
        autoFocus
        id="name"
        variant={INPUT_VARIANTS.SECONDARY}
        label="Model Name (no spaces - lowercase, numbers, and dashes only):"
        type="text"
        value={state.name}
        onChange={({ target: { value } }) => {
          dispatch({ name: slugify({ value }) })
        }}
        hasError={state.errors.name !== undefined}
        errorMessage={state.errors.name}
      />

      <Textarea
        id="description"
        variant={TEXTAREA_VARIANTS.SECONDARY}
        label="Description (optional):"
        value={state.description}
        onChange={({ target: { value } }) => {
          dispatch({ description: value })
        }}
        hasError={state.errors.description !== undefined}
        errorMessage={state.errors.description}
      />

      {/**
      <SectionTitle>Select Training</SectionTitle>

      {SOURCES.map(({ value, label }) => {
        return (
          <div key={value} css={{ paddingTop: spacing.normal }}>
            <Radio
              name="source"
              option={{
                value,
                label,
                legend: '',
                initialValue: state.source === value,
              }}
              onClick={({ value: source }) =>
                dispatch({
                  source,
                  type:
                    source === SOURCES[1].value ? PRE_TRAINED_MODEL_TYPE : '',
                })
              }
            />
          </div>
        )
      })}
      */}

      <SectionTitle>Select Model Type</SectionTitle>

      {modelTypes.map(({ name, label, description }) => {
        if (name === PRE_TRAINED_MODEL_TYPE) return null

        return (
          <div key={name} css={{ paddingTop: spacing.normal }}>
            <Radio
              name="modelType"
              option={{
                value: name,
                label,
                legend: description,
                initialValue: state.type === name,
              }}
              onClick={({ value }) => dispatch({ type: value })}
            />
          </div>
        )
      })}

      <ButtonGroup>
        <Button
          type="submit"
          variant={BUTTON_VARIANTS.PRIMARY}
          onClick={() => onSubmit({ dispatch, projectId, state })}
          isDisabled={!state.name || !state.type || state.isLoading}
        >
          {state.isLoading ? 'Creating...' : 'Create New Model'}
        </Button>
      </ButtonGroup>
    </Form>
  )
}

export default ModelsAddForm
