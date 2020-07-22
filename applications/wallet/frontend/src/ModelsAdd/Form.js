import { useReducer } from 'react'
import { useRouter } from 'next/router'
import useSWR from 'swr'

import Form from '../Form'
import Input, { VARIANTS as INPUT_VARIANTS } from '../Input'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import ButtonGroup from '../Button/Group'
import Select from '../Select'

import { onSubmit } from './helpers'

const INITIAL_STATE = {
  type: '',
  name: '',
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

  const options = modelTypes.map(({ name }) => ({ key: name, value: name }))

  return (
    <Form>
      <Select
        name="model-types"
        label="Model Type"
        options={options}
        onChange={({ value }) => {
          dispatch({ type: value })
        }}
      />

      <Input
        autoFocus
        id="name"
        variant={INPUT_VARIANTS.SECONDARY}
        label="Name"
        type="text"
        value={state.name}
        onChange={({ target: { value } }) => dispatch({ name: value })}
        hasError={state.errors.name !== undefined}
        errorMessage={state.errors.name}
      />

      <ButtonGroup>
        <Button
          type="submit"
          variant={BUTTON_VARIANTS.PRIMARY}
          onClick={() => onSubmit({ dispatch, projectId, state })}
          isDisabled={!state.name || state.isLoading}
        >
          {state.isLoading ? 'Creating...' : 'Create New Model'}
        </Button>
      </ButtonGroup>
    </Form>
  )
}

export default ModelsAddForm
