import { useReducer } from 'react'
import { useRouter } from 'next/router'
import useSWR from 'swr'

import { spacing, typography } from '../Styles'

import Form from '../Form'
import Input, { VARIANTS as INPUT_VARIANTS } from '../Input'
import CheckboxGroup from '../Checkbox/Group'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'

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

  if (!Array.isArray(permissions)) return 'Loading...'

  return (
    <Form>
      <h2
        css={{
          fontSize: typography.size.medium,
          lineHeight: typography.height.medium,
          fontWeight: typography.weight.medium,
          paddingTop: spacing.normal,
          paddingBottom: spacing.normal,
        }}>
        Invite User to view projects
      </h2>

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
          legend: description,
          initialValue: false,
        }))}
      />

      <div
        css={{
          paddingTop: spacing.moderate,
          paddingBottom: spacing.moderate,
        }}>
        <Button
          type="submit"
          variant={BUTTON_VARIANTS.PRIMARY}
          onClick={console.warn}
          isDisabled={!state.email}>
          Send Invite
        </Button>
      </div>
    </Form>
  )
}

export default ProjectUsersAddForm
