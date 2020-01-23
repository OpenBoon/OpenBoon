import { useReducer } from 'react'
import PropTypes from 'prop-types'
import { useRouter } from 'next/router'
import useSWR from 'swr'

import { spacing } from '../Styles'

import Form from '../Form'
import Input from '../Input'
import CheckboxGroup from '../Checkbox/Group'
import Button, { VARIANTS } from '../Button'

const reducer = (state, action) => ({ ...state, ...action })

const ApiKeysAddForm = ({ onSubmit }) => {
  const {
    query: { projectId },
  } = useRouter()

  const { data: { results: permissions } = {} } = useSWR(
    `/api/v1/projects/${projectId}/permissions/`,
  )

  const [state, dispatch] = useReducer(reducer, { name: '' })

  if (!Array.isArray(permissions)) return 'Loading...'

  return (
    <Form>
      <Input
        autoFocus
        id="name"
        label="Name"
        type="text"
        value={state.name}
        onChange={({ target: { value } }) => dispatch({ name: value })}
        hasError={false}
      />

      <div>&nbsp;</div>

      <CheckboxGroup
        legend="Add Scope"
        onClick={dispatch}
        options={permissions.map(({ name, description }, index) => ({
          key: name,
          label: name.replace(/([A-Z])/g, match => ` ${match}`),
          legend: description,
          initialValue: index === 0,
        }))}
      />
      <div
        css={{
          paddingTop: spacing.moderate,
          paddingBottom: spacing.moderate,
        }}>
        <Button
          type="submit"
          variant={VARIANTS.PRIMARY}
          onClick={() => onSubmit(state)}
          isDisabled={!state.name}>
          Generate Key &amp; Download
        </Button>
      </div>
    </Form>
  )
}

ApiKeysAddForm.propTypes = {
  onSubmit: PropTypes.func.isRequired,
}

export default ApiKeysAddForm
