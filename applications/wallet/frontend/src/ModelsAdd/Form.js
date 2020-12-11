import { useReducer } from 'react'
import { useRouter } from 'next/router'
import useSWR from 'swr'

import { spacing } from '../Styles'

import Form from '../Form'
import Input, { VARIANTS as INPUT_VARIANTS } from '../Input'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import Select, { VARIANTS as SELECT_VARIANTS } from '../Select'
import FlashMessageErrors from '../FlashMessage/Errors'
import ButtonGroup from '../Button/Group'

import { onSubmit, slugify } from './helpers'

const INITIAL_STATE = {
  type: '',
  name: '',
  moduleName: '',
  isCustomModuleName: false,
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

  const options = modelTypes.map(({ name }) => ({ value: name, label: name }))

  return (
    <Form>
      <FlashMessageErrors
        errors={state.errors}
        styles={{ marginTop: -spacing.base, paddingBottom: spacing.comfy }}
      />
      <Select
        label="Model Type"
        options={options}
        onChange={({ value }) => {
          dispatch({ type: value })
        }}
        variant={SELECT_VARIANTS.COLUMN}
        isRequired={false}
      />

      <Input
        autoFocus
        id="name"
        variant={INPUT_VARIANTS.SECONDARY}
        label="Name"
        type="text"
        value={state.name}
        onChange={({ target: { value } }) => {
          dispatch({
            name: value,
            moduleName: state.isCustomModuleName
              ? state.moduleName
              : slugify({ value }),
          })
        }}
        hasError={state.errors.name !== undefined}
        errorMessage={state.errors.name}
      />

      <Input
        id="moduleName"
        variant={INPUT_VARIANTS.SECONDARY}
        label="Module Name"
        type="text"
        value={state.moduleName}
        onChange={({ target: { value } }) => {
          dispatch({ moduleName: value, isCustomModuleName: !!value })
        }}
        hasError={state.errors.moduleName !== undefined}
        errorMessage={state.errors.moduleName}
      />

      <ButtonGroup>
        <Button
          type="submit"
          variant={BUTTON_VARIANTS.PRIMARY}
          onClick={() => onSubmit({ dispatch, projectId, state })}
          isDisabled={
            !state.type || !state.name || !state.moduleName || state.isLoading
          }
        >
          {state.isLoading ? 'Creating...' : 'Create New Model'}
        </Button>
      </ButtonGroup>
    </Form>
  )
}

export default ModelsAddForm
