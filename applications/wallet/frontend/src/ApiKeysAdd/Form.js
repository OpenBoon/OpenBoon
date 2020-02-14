import { useReducer } from 'react'
import { useRouter } from 'next/router'
import useSWR from 'swr'

import Loading from '../Loading'
import Form from '../Form'
import Input, { VARIANTS as INPUT_VARIANTS } from '../Input'
import { VARIANTS as CHECKBOX_VARIANTS } from '../Checkbox'
import { VARIANTS as CHECKBOX_ICON_VARIANTS } from '../Checkbox/Icon'
import CheckboxGroup from '../Checkbox/Group'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import ButtonGroup from '../Button/Group'

import { onSubmit } from './helpers'

import ApiKeysAddFormSuccess from './FormSuccess'

const INITIAL_STATE = {
  name: '',
  permissions: {},
  apikey: {},
  errors: {},
}

const reducer = (state, action) => ({ ...state, ...action })

const ApiKeysAddForm = () => {
  const {
    query: { projectId },
  } = useRouter()

  const { data: { results: permissions } = {} } = useSWR(
    `/api/v1/projects/${projectId}/permissions/`,
  )

  const [state, dispatch] = useReducer(reducer, INITIAL_STATE)

  if (!Array.isArray(permissions)) return <Loading />

  const { apikey } = state

  if (apikey.secretKey) {
    return (
      <ApiKeysAddFormSuccess
        projectId={projectId}
        apikey={apikey}
        onReset={() => dispatch(INITIAL_STATE)}
      />
    )
  }

  return (
    <Form>
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
        onClick={permission =>
          dispatch({ permissions: { ...state.permissions, ...permission } })
        }
        options={permissions.map(({ name, description }) => ({
          key: name,
          label: name.replace(/([A-Z])/g, match => ` ${match}`),
          icon: '',
          legend: description,
          initialValue: false,
        }))}
        variant={CHECKBOX_VARIANTS.PRIMARY}
        iconVariant={CHECKBOX_ICON_VARIANTS.PRIMARY}
      />

      <ButtonGroup>
        <Button
          type="submit"
          variant={BUTTON_VARIANTS.PRIMARY}
          onClick={() => onSubmit({ dispatch, projectId, state })}
          isDisabled={!state.name}>
          Generate Key &amp; Download
        </Button>
      </ButtonGroup>
    </Form>
  )
}

export default ApiKeysAddForm
