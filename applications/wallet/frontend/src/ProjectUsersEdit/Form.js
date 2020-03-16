import { useReducer } from 'react'
import PropTypes from 'prop-types'
import Link from 'next/link'
import useSWR from 'swr'

import Form from '../Form'
import SectionTitle from '../SectionTitle'
import FormAlert from '../FormAlert'
import { VARIANTS as CHECKBOX_VARIANTS } from '../Checkbox'
import CheckboxGroup from '../Checkbox/Group'
import Button, { VARIANTS } from '../Button'
import ButtonGroup from '../Button/Group'

import { onSubmit } from './helpers'

const reducer = (state, action) => ({ ...state, ...action })

const ProjectUsersEditForm = ({ projectId, userId }) => {
  const { data: user } = useSWR(`/api/v1/projects/${projectId}/users/${userId}`)

  const {
    data: { results: permissions },
  } = useSWR(`/api/v1/projects/${projectId}/permissions/`)

  const [state, dispatch] = useReducer(reducer, {
    permissions: user.permissions.reduce((accumulator, permission) => {
      accumulator[permission] = true
      return accumulator
    }, {}),
    error: '',
  })

  return (
    <Form>
      <SectionTitle>Email: {user.email}</SectionTitle>

      <FormAlert setErrorMessage={() => dispatch({ error: '' })}>
        {state.error}
      </FormAlert>

      <CheckboxGroup
        legend="Edit Permissions"
        onClick={permission =>
          dispatch({ permissions: { ...state.permissions, ...permission } })
        }
        options={permissions.map(({ name, description }) => ({
          value: name,
          label: name.replace(/([A-Z])/g, match => ` ${match}`),
          icon: '',
          legend: description,
          initialValue: !!user.permissions.includes(name),
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
          isDisabled={false}>
          Save
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
