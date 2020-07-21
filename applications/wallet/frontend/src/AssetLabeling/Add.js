import { useReducer } from 'react'
import useSWR from 'swr'
import PropTypes from 'prop-types'

import { spacing } from '../Styles'

import Form from '../Form'
import Input, { VARIANTS as INPUT_VARIANTS } from '../Input'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import Select from '../Select'

const noop = () => {}

const INITIAL_STATE = {
  model: '',
  label: '',
  isLoading: false,
  errors: {},
}

const reducer = (state, action) => ({ ...state, ...action })

const AssetLabelingAdd = ({ projectId }) => {
  const {
    data: { results: models },
  } = useSWR(`/api/v1/projects/${projectId}/models/`)

  const [state, dispatch] = useReducer(reducer, INITIAL_STATE)

  return (
    <div css={{ padding: spacing.normal }}>
      <Form style={{ width: '100%', padding: 0 }}>
        <Select
          htmlFor="models-list"
          label="Model:"
          placeholder="Select a model..."
          onChange={({ target: { value } }) => {
            dispatch({ model: value })
          }}
          style={{ width: '100%' }}
        >
          {models.map((option) => {
            return (
              <option key={option.name} value={option.id}>
                {option.name}
              </option>
            )
          })}
        </Select>

        <Input
          id="asset-label"
          variant={INPUT_VARIANTS.SECONDARY}
          label="Label:"
          type="text"
          value={state.label}
          onChange={({ target: { value } }) => dispatch({ label: value })}
          hasError={state.errors.label !== undefined}
          errorMessage={state.errors.label}
          style={{ width: '100%' }}
        />

        <div css={{ display: 'flex' }}>
          <Button
            variant={BUTTON_VARIANTS.SECONDARY}
            onClick={noop}
            style={{ flex: 1, margin: 0 }}
          >
            Cancel
          </Button>
          <div css={{ padding: spacing.base }} />
          <Button
            type="submit"
            variant={BUTTON_VARIANTS.PRIMARY}
            onClick={noop}
            isDisabled={!state.label || state.isLoading}
            style={{ flex: 1, margin: 0 }}
          >
            Save Label
          </Button>
        </div>
      </Form>
    </div>
  )
}

AssetLabelingAdd.propTypes = {
  projectId: PropTypes.string.isRequired,
}

export default AssetLabelingAdd
