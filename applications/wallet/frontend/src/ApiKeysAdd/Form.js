import { useReducer } from 'react'
import { useRouter } from 'next/router'
import useSWR from 'swr'

import { spacing } from '../Styles'

import Form from '../Form'
import FlashMessageErrors from '../FlashMessage/Errors'
import Input, { VARIANTS as INPUT_VARIANTS } from '../Input'
import { VARIANTS as CHECKBOX_VARIANTS } from '../Checkbox'
import CheckboxGroup from '../Checkbox/Group'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import ButtonGroup from '../Button/Group'

import { onSubmit } from './helpers'

import ApiKeysAddFormSuccess from './FormSuccess'

const INITIAL_STATE = {
  name: '',
  permissions: {},
  apikey: {},
  isLoading: false,
  errors: {},
}

const reducer = (state, action) => ({ ...state, ...action })

const ApiKeysAddForm = () => {
  const {
    query: { projectId },
  } = useRouter()

  const {
    data: { results: permissions },
  } = useSWR(`/api/v1/projects/${projectId}/permissions/`)

  const [state, dispatch] = useReducer(reducer, INITIAL_STATE)

  const { apikey } = state

  if (apikey.secretKey) {
    return (
      <ApiKeysAddFormSuccess
        projectId={projectId}
        permissions={Object.keys(state.permissions).filter(
          (key) => state.permissions[key],
        )}
        apikey={apikey}
        name={state.name}
        onReset={() => dispatch(INITIAL_STATE)}
      />
    )
  }

  return (
    <Form>
      <FlashMessageErrors
        errors={state.errors}
        styles={{ marginTop: -spacing.base, paddingBottom: spacing.normal }}
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

      <CheckboxGroup
        legend="Add Scope"
        description=""
        variant={CHECKBOX_VARIANTS.PRIMARY}
        options={permissions
          .sort((a, b) => {
            if (a.name.toLowerCase() < b.name.toLowerCase()) return -1
            return 1
          })
          .map(({ name, description }) => ({
            value: name,
            label: name.replace(/([A-Z])/g, (match) => ` ${match}`),
            icon: '',
            legend: description,
            initialValue: false,
            isDisabled: false,
          }))}
        onClick={(permission) =>
          dispatch({ permissions: { ...state.permissions, ...permission } })
        }
      />

      <ButtonGroup>
        <Button
          type="submit"
          variant={BUTTON_VARIANTS.PRIMARY}
          onClick={() => onSubmit({ dispatch, projectId, state })}
          isDisabled={!state.name || state.isLoading}
        >
          {state.isLoading ? 'Generating...' : 'Generate Key & Download'}
        </Button>
      </ButtonGroup>
    </Form>
  )
}

export default ApiKeysAddForm
