import { useReducer } from 'react'
import { useRouter } from 'next/router'
import useSWR from 'swr'

import Form from '../Form'
import SectionTitle from '../SectionTitle'
import Input, { VARIANTS as INPUT_VARIANTS } from '../Input'
import { VARIANTS as CHECKBOX_VARIANTS } from '../Checkbox'
import CheckboxGroup from '../Checkbox/Group'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import ButtonGroup from '../Button/Group'
import Loading from '../Loading'

import { onSubmit } from './helpers'

import ProjectUsersAddFormResponse from './FormResponse'

const INITIAL_STATE = {
  emails: '',
  permissions: {},
  succeeded: [],
  failed: [],
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

  if (state.succeeded.length > 0 || state.failed.length > 0) {
    return (
      <ProjectUsersAddFormResponse
        projectId={projectId}
        succeeded={state.succeeded}
        failed={state.failed}
        permissions={state.permissions}
        onReset={() => dispatch(INITIAL_STATE)}
      />
    )
  }

  return (
    <Form>
      <SectionTitle>Invite User to view projects</SectionTitle>

      <Input
        autoFocus
        id="emails"
        variant={INPUT_VARIANTS.SECONDARY}
        label="Email(s)"
        type="text"
        value={state.emails}
        onChange={({ target: { value } }) => dispatch({ emails: value })}
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
      />

      <ButtonGroup>
        <Button
          type="submit"
          variant={BUTTON_VARIANTS.PRIMARY}
          onClick={() => onSubmit({ projectId, dispatch, state })}
          isDisabled={!state.emails}>
          Send Invite
        </Button>
      </ButtonGroup>
    </Form>
  )
}

export default ProjectUsersAddForm
