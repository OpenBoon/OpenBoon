import { useReducer } from 'react'
import PropTypes from 'prop-types'

import modelShape from '../Model/shape'

import { spacing } from '../Styles'

import { useLocalStorage } from '../LocalStorage/helpers'
import Form from '../Form'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import FlashMessageErrors from '../FlashMessage/Errors'
import Select from '../Select'
import Combobox from '../Combobox'

import { onSubmit, getSubmitText, getOptions } from './helpers'

import AssetLabelingShortcuts from './Shortcuts'

const INITIAL_STATE = {
  success: false,
  isLoading: false,
  errors: {},
  reloadKey: 0,
}

const reducer = (state, action) => ({ ...state, ...action })

const AssetLabelingAdd = ({ projectId, assetId, models, labels }) => {
  const [localModelId, setLocalModelId] = useLocalStorage({
    key: `AssetLabelingAdd.${projectId}.modelId`,
    initialState: '',
  })

  const [localLabel, setLocalLabel] = useLocalStorage({
    key: `AssetLabelingAdd.${projectId}.label`,
    initialState: '',
  })

  const hasModel = models.find(({ id }) => id === localModelId)

  const [state, dispatch] = useReducer(reducer, {
    ...INITIAL_STATE,
    modelId: (hasModel && localModelId) || '',
    label: localLabel || '',
  })

  const options = models.map(({ name, id }) => ({ value: id, label: name }))

  const existingLabel = labels.find(
    ({ modelId, label }) => modelId === state.modelId && label === state.label,
  )

  return (
    <div css={{ padding: spacing.normal }}>
      <AssetLabelingShortcuts
        dispatch={dispatch}
        state={state}
        labels={labels}
        projectId={projectId}
        assetId={assetId}
      />

      <Form style={{ width: '100%', padding: 0 }}>
        <FlashMessageErrors
          errors={state.errors}
          styles={{ paddingTop: spacing.base, paddingBottom: spacing.comfy }}
        />
        <Select
          key={`model${state.reloadKey}`}
          label="Model"
          options={options}
          defaultValue={state.modelId}
          onChange={({ value }) => {
            dispatch({ modelId: value, label: '', success: false })
            setLocalModelId({ value })
          }}
          isRequired={false}
          style={{ width: '100%' }}
        />

        <div css={{ height: spacing.base }} />

        <Combobox
          key={`label${state.reloadKey}${state.modelId}`}
          label="Label"
          options={() => getOptions({ projectId, modelId: state.modelId })}
          value={state.label}
          onChange={({ value }) => {
            dispatch({ label: value, success: false })
            setLocalLabel({ value })
          }}
          hasError={state.errors.label !== undefined}
          errorMessage={state.errors.label}
        />

        <div css={{ height: spacing.comfy }} />

        <div css={{ display: 'flex' }}>
          <Button
            variant={BUTTON_VARIANTS.SECONDARY}
            onClick={() => {
              dispatch({
                modelId: localModelId,
                label: localLabel,
                reloadKey: state.reloadKey + 1,
              })
            }}
            style={{ flex: 1, margin: 0 }}
            isDisabled={
              !localModelId ||
              !localLabel ||
              (localModelId &&
                localLabel &&
                localModelId === state.modelId &&
                localLabel === state.label)
            }
          >
            Cancel
          </Button>

          <div css={{ padding: spacing.base }} />

          <Button
            type="submit"
            variant={BUTTON_VARIANTS.PRIMARY}
            onClick={() => {
              onSubmit({ dispatch, state, labels, projectId, assetId })
            }}
            isDisabled={
              !hasModel ||
              !state.modelId ||
              (!state.label && !localLabel) ||
              state.isLoading ||
              (state.success && !state.isLoading) ||
              !!existingLabel
            }
            style={{ flex: 1, margin: 0 }}
          >
            {getSubmitText({ state, existingLabel })}
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
  labels: PropTypes.arrayOf(
    PropTypes.shape({
      label: PropTypes.string,
      modelId: PropTypes.string,
    }),
  ).isRequired,
}

export default AssetLabelingAdd
