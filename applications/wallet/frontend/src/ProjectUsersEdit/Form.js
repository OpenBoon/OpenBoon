import { useReducer } from 'react'
import PropTypes from 'prop-types'
import Link from 'next/link'

import Form from '../Form'
import SectionTitle from '../SectionTitle'
import FormAlert from '../FormAlert'
import { VARIANTS as CHECKBOX_VARIANTS } from '../Checkbox'
import CheckboxGroup from '../Checkbox/Group'
import Button, { VARIANTS } from '../Button'
import ButtonGroup from '../Button/Group'

import { onSubmit } from './helpers'

const reducer = (state, action) => ({ ...state, ...action })

const ProjectUsersEditForm = ({ projectId, user, permissions }) => {
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

      <FormAlert
        errorMessage={state.error}
        setErrorMessage={() => dispatch({ error: '' })}
      />

      <CheckboxGroup
        legend="Edit Permissions"
        onClick={permission =>
          dispatch({ permissions: { ...state.permissions, ...permission } })
        }
        options={permissions.map(({ name, description }) => ({
          key: name,
          label: name.replace(/([A-Z])/g, match => ` ${match}`),
          icon: '',
          legend: description,
          initialValue: !!user.permissions.includes(name),
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
          isDisabled={
            !Object.values(state.permissions).filter(Boolean).length > 0
          }>
          Save
        </Button>
      </ButtonGroup>
    </Form>
  )
}

ProjectUsersEditForm.propTypes = {
  projectId: PropTypes.string.isRequired,
  user: PropTypes.shape({
    id: PropTypes.string.isRequired,
    email: PropTypes.string.isRequired,
    permissions: PropTypes.arrayOf(PropTypes.string.isRequired).isRequired,
  }).isRequired,
  permissions: PropTypes.arrayOf(
    PropTypes.shape({
      name: PropTypes.string.isRequired,
      description: PropTypes.string.isRequired,
    }).isRequired,
  ).isRequired,
}

export default ProjectUsersEditForm
