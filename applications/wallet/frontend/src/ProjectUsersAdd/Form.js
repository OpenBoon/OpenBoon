import { useReducer } from 'react'
import { useRouter } from 'next/router'
import useSWR from 'swr'

import Form from '../Form'
import SectionTitle from '../SectionTitle'
import Input, { VARIANTS as INPUT_VARIANTS } from '../Input'
import { VARIANTS as CHECKBOX_VARIANTS } from '../Checkbox'
import { VARIANTS as CHECKBOX_ICON_VARIANTS } from '../Checkbox/Icon'
import CheckboxGroup from '../Checkbox/Group'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import ButtonGroup from '../Button/Group'
import Loading from '../Loading'

const INITIAL_STATE = {
  email: '',
  permissions: {},
  errors: {},
}

const reducer = (state, action) => ({ ...state, ...action })

const ProjectUsersAddForm = () => {
  const {
    query: { projectId },
  } = useRouter()

  const { data: { results: permissions } = {} } = useSWR(
    `/api/v1/projects/${projectId}/permissions/`,
  )

  const [state, dispatch] = useReducer(reducer, INITIAL_STATE)

  if (!Array.isArray(permissions)) return <Loading />

  return (
    <Form>
      <SectionTitle>Invite User to view projects</SectionTitle>

      <Input
        autoFocus
        id="email"
        variant={INPUT_VARIANTS.SECONDARY}
        label="Email(s)"
        type="text"
        value={state.email}
        onChange={({ target: { value } }) => dispatch({ email: value })}
        hasError={state.errors.name !== undefined}
        errorMessage={state.errors.name}
      />

      <CheckboxGroup
        legend="Add Permissions"
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
          onClick={console.warn}
          isDisabled={!state.email}>
          Send Invite
        </Button>
      </ButtonGroup>
    </Form>
  )
}

export default ProjectUsersAddForm
