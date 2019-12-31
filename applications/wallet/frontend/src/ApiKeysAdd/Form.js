import { useReducer } from 'react'
import PropTypes from 'prop-types'

import { spacing } from '../Styles'

import Input from '../Input'
import CheckboxGroup from '../Checkbox/Group'
import Button, { VARIANTS } from '../Button'

const reducer = (state, action) => ({ ...state, ...action })

const ApiKeysAddForm = ({ onSubmit }) => {
  const [state, dispatch] = useReducer(reducer, { name: '' })

  return (
    <form
      method="post"
      onSubmit={event => event.preventDefault()}
      css={{
        display: 'flex',
        flexDirection: 'column',
      }}>
      <Input
        autoFocus
        id="name"
        label="Name"
        type="text"
        value={state.name}
        onChange={({ target: { value } }) => dispatch({ name: value })}
        hasError={false}
      />
      <CheckboxGroup
        legend="Permissions"
        onClick={dispatch}
        options={[
          {
            key: 'api',
            label: 'API',
            legend: "Dude You're Getting A Telescope",
            initialValue: true,
          },
          {
            key: 'kuphalfort',
            label: 'Kuphalfort',
            legend: 'Space The Final Frontier',
            initialValue: false,
          },
          {
            key: 'west_courtney',
            label: 'West Courtney',
            legend: 'Astronomy Binoculars A Great Alternative',
            initialValue: false,
          },
          {
            key: 'murphyside',
            label: 'Murphyside',
            legend: 'Asteroids',
            initialValue: false,
          },
        ]}
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
    </form>
  )
}

ApiKeysAddForm.propTypes = {
  onSubmit: PropTypes.func.isRequired,
}

export default ApiKeysAddForm
