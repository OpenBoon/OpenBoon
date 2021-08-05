import { useReducer } from 'react'
import PropTypes from 'prop-types'
import Link from 'next/link'
import useSWR from 'swr'

import { spacing } from '../Styles'

import Form from '../Form'
import SectionTitle from '../SectionTitle'
import FlashMessageErrors from '../FlashMessage/Errors'
import { VARIANTS as CHECKBOX_VARIANTS } from '../Checkbox'
import CheckboxGroup from '../Checkbox/Group'
import Button, { VARIANTS } from '../Button'
import ButtonGroup from '../Button/Group'

import { onSubmit } from './helpers'

const reducer = (state, action) => ({ ...state, ...action })

const ProjectUsersEditForm = ({ projectId, userId }) => {
  const { data: user } = useSWR(`/api/v1/projects/${projectId}/users/${userId}`)

  const {
    data: { results: roles },
  } = useSWR(`/api/v1/projects/${projectId}/roles/`)

  const [state, dispatch] = useReducer(reducer, {
    roles: user.roles.reduce((accumulator, role) => {
      accumulator[role] = true
      return accumulator
    }, {}),
    isLoading: false,
    errors: {},
  })

  return (
    <Form>
      <FlashMessageErrors
        errors={state.errors}
        styles={{ marginTop: -spacing.base, paddingBottom: spacing.base }}
      />

      <SectionTitle>Email: {user.email}</SectionTitle>

      <CheckboxGroup
        legend="Edit Roles"
        description=""
        onClick={(role) => dispatch({ roles: { ...state.roles, ...role } })}
        options={roles.map(({ name, description }) => ({
          value: name,
          label: name.replaceAll('_', ' '),
          icon: '',
          legend: description,
          initialValue: !!user.roles.includes(name),
          isDisabled: false,
        }))}
        variant={CHECKBOX_VARIANTS.PRIMARY}
      />

      <ButtonGroup>
        <Link href="/[projectId]/users" as={`/${projectId}/users`} passHref>
          <Button variant={VARIANTS.SECONDARY}>Cancel</Button>
        </Link>
        <Button
          type="submit"
          variant={VARIANTS.PRIMARY}
          onClick={() =>
            onSubmit({ dispatch, projectId, userId: user.id, state })
          }
          isDisabled={state.isLoading}
        >
          {state.isLoading ? 'Saving...' : 'Save'}
        </Button>
      </ButtonGroup>
    </Form>
  )
}

ProjectUsersEditForm.propTypes = {
  projectId: PropTypes.string.isRequired,
  userId: PropTypes.number.isRequired,
}

export default ProjectUsersEditForm
