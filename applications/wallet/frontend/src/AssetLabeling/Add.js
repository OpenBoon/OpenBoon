import { useReducer } from 'react'
import PropTypes from 'prop-types'

import modelShape from '../Model/shape'

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
  reloadKey: 0,
}

const reducer = (state, action) => ({ ...state, ...action })

const AssetLabelingAdd = ({ projectId, assetId, models }) => {
  const [localModel, setLocalModel] = useLocalStorageState({
    key: 'AssetLabeling.Add.Model',
    initialValue: '',
  })
  const [localLabel, setLocalLabel] = useLocalStorageState({
    key: 'AssetLabeling.Add.Label',
    initialValue: '',
  })

  // Prevents user from saving non-existent moel where model/label
  // are added to local storage and user switches projects
  const modelExists = localModel
    ? models.find(({ id }) => id === localModel)
    : false

  const [state, dispatch] = useReducer(reducer, {
    ...INITIAL_STATE,
    model: modelExists ? localModel : '',
    label: modelExists ? localLabel : '',
  })

  const options = models.map(({ name, id }) => ({ value: id, label: name }))

  return (
    <div css={{ padding: spacing.normal }}>
      <Form style={{ width: '100%', padding: 0 }}>
        <Select
          key={state.reloadKey}
          label="Model"
          options={options}
          defaultValue={state.model}
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
          value={state.label}
          onChange={({ target: { value } }) => dispatch({ label: value })}
          hasError={state.errors.label !== undefined}
          errorMessage={state.errors.label}
          style={{ width: '100%' }}
        />

        <div css={{ display: 'flex' }}>
          <Button
            variant={BUTTON_VARIANTS.SECONDARY}
            onClick={() =>
              dispatch({
                model: localModel,
                label: localLabel,
                reloadKey: state.reloadKey + 1,
              })
            }
            style={{ flex: 1, margin: 0 }}
            isDisabled={
              !localModel ||
              !localLabel ||
              (localModel &&
                localLabel &&
                localModel === state.model &&
                localLabel === state.label)
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
  models: PropTypes.arrayOf(modelShape).isRequired,
}

export default AssetLabelingAdd
