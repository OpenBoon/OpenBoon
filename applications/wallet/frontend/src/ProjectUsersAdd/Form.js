import { useReducer } from 'react'
import { useRouter } from 'next/router'
import useSWR from 'swr'

import Form from '../Form'
import SectionTitle from '../SectionTitle'
import FormAlert from '../FormAlert'
import Input, { VARIANTS as INPUT_VARIANTS } from '../Input'
import { VARIANTS as CHECKBOX_VARIANTS } from '../Checkbox'
import CheckboxGroup from '../Checkbox/Group'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import ButtonGroup from '../Button/Group'

import { onSubmit } from './helpers'

import ProjectUsersAddFormResponse from './FormResponse'
import ProjectUsersAddCopyLink from './CopyLink'

const INITIAL_STATE = {
  emails: '',
  roles: {},
  succeeded: [],
  failed: [],
  errors: { global: '' },
}

const reducer = (state, action) => ({ ...state, ...action })

const ProjectUsersAddForm = () => {
  const {
    query: { projectId },
  } = useRouter()

  const {
    data: { results: roles },
  } = useSWR(`/api/v1/projects/${projectId}/roles/`)

  const [state, dispatch] = useReducer(reducer, INITIAL_STATE)

  if (state.succeeded.length > 0 || state.failed.length > 0) {
    return (
      <ProjectUsersAddFormResponse
        projectId={projectId}
        succeeded={state.succeeded}
        failed={state.failed}
        roles={state.roles}
        onReset={() => dispatch(INITIAL_STATE)}
      />
    )
  }

  return (
    <div>
      <SectionTitle>Add User(s) to Project</SectionTitle>

      <ProjectUsersAddCopyLink />

      <Form>
        <FormAlert
          setErrorMessage={() =>
            dispatch({ errors: { ...state.errors, global: '' } })
          }
        >
          {state.errors.global}
        </FormAlert>

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
          legend="Add Roles"
          description=""
          onClick={(role) => dispatch({ roles: { ...state.roles, ...role } })}
          options={roles.map(({ name, description }) => ({
            value: name,
            label: name.replace('_', ' '),
            icon: '',
            legend: description,
            initialValue: false,
            isDisabled: false,
          }))}
          variant={CHECKBOX_VARIANTS.PRIMARY}
        />

        <ButtonGroup>
          <Button
            type="submit"
            variant={BUTTON_VARIANTS.PRIMARY}
            onClick={() => onSubmit({ projectId, dispatch, state })}
            isDisabled={!state.emails}
          >
            Add
          </Button>
        </ButtonGroup>
      </Form>
    </div>
  )
}

export default ProjectUsersAddForm
