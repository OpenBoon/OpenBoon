import { useReducer } from 'react'
import useSWR from 'swr'
import PropTypes from 'prop-types'

import { spacing } from '../Styles'

import { useLocalStorageState } from '../LocalStorage/helpers'
import Form from '../Form'
import Input, { VARIANTS as INPUT_VARIANTS } from '../Input'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import Select from '../Select'

import { onSubmit, getSubmitText } from './helpers'

const INITIAL_STATE = {
  success: false,
  isLoading: false,
  errors: {},
}

const reducer = (state, action) => ({ ...state, ...action })

const AssetLabelingAdd = ({ projectId, assetId }) => {
  const {
    data: { results: models },
  } = useSWR(`/api/v1/projects/${projectId}/models/`)

  const [localModel, setLocalModel] = useLocalStorageState({
    key: 'AssetLabeling.Add.Model',
    initialValue: '',
  })
  const [localLabel, setLocalLabel] = useLocalStorageState({
    key: 'AssetLabeling.Add.Label',
    initialValue: '',
  })

  const [state, dispatch] = useReducer(reducer, {
    ...INITIAL_STATE,
    model: localModel || '',
    label: localLabel || '',
  })

  const options = models.map(({ name, id }) => ({ value: id, label: name }))

  return (
    <div css={{ padding: spacing.normal }}>
      <Form style={{ width: '100%', padding: 0 }}>
        <Select
          label="Model"
          options={options}
          defaultValue={localModel}
          onChange={({ value }) => {
            dispatch({ model: value })
          }}
          isRequired={false}
          style={{ width: '100%' }}
        />

        <Input
          id="asset-label"
          variant={INPUT_VARIANTS.SECONDARY}
          label="Label"
          type="text"
          value={state.label || localLabel}
          onChange={({ target: { value } }) => dispatch({ label: value })}
          hasError={state.errors.label !== undefined}
          errorMessage={state.errors.label}
          style={{ width: '100%' }}
        />

        <div css={{ display: 'flex' }}>
          <Button
            variant={BUTTON_VARIANTS.SECONDARY}
            onClick={() => dispatch({ model: localModel, label: localLabel })}
            style={{ flex: 1, margin: 0 }}
            isDisabled={
              !localModel ||
              !localLabel ||
              (localLabel && localLabel === state.label)
            }
          >
            Cancel
          </Button>
          <div css={{ padding: spacing.base }} />
          <Button
            type="submit"
            variant={BUTTON_VARIANTS.PRIMARY}
            onClick={() =>
              onSubmit({
                dispatch,
                state,
                projectId,
                assetId,
                setLocalModel,
                setLocalLabel,
              })
            }
            isDisabled={
              (!state.model && !localModel) ||
              (!state.label && !localLabel) ||
              state.isLoading
            }
            style={{ flex: 1, margin: 0 }}
          >
            {getSubmitText({ state })}
          </Button>
        </div>
      </Form>
    </div>
  )
}

AssetLabelingAdd.propTypes = {
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
}

export default AssetLabelingAdd
