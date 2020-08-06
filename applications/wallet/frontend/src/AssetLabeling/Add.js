import { useReducer } from 'react'
import PropTypes from 'prop-types'

import modelShape from '../Model/shape'

import { spacing } from '../Styles'

import { useLocalStorageState } from '../LocalStorage/helpers'
import Form from '../Form'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import FlashMessage, { VARIANTS as FLASH_VARIANTS } from '../FlashMessage'
import Select from '../Select'
import Combobox from '../Combobox'

import { onSubmit, getSubmitText, getOptions } from './helpers'

const INITIAL_STATE = {
  success: false,
  isLoading: false,
  errors: {},
  reloadKey: 0,
}

const reducer = (state, action) => ({ ...state, ...action })

const AssetLabelingAdd = ({ projectId, assetId, models, labels }) => {
  const [localModelId, setLocalModelId] = useLocalStorageState({
    key: `AssetLabelingAdd.${projectId}.modelId`,
    initialValue: '',
  })
  const [localLabel, setLocalLabel] = useLocalStorageState({
    key: `AssetLabelingAdd.${projectId}.label`,
    initialValue: '',
  })

  const [state, dispatch] = useReducer(reducer, {
    ...INITIAL_STATE,
    modelId: localModelId || '',
    label: localLabel || '',
  })

  const options = models.map(({ name, id }) => ({ value: id, label: name }))

  const existingLabel = labels.find(
    ({ modelId, label }) => modelId === state.modelId && label === state.label,
  )

  return (
    <div css={{ padding: spacing.normal }}>
      {state.errors.global && (
        <div css={{ paddingBottom: spacing.normal }}>
          <FlashMessage variant={FLASH_VARIANTS.ERROR}>
            {state.errors.global}
          </FlashMessage>
        </div>
      )}
      <Form style={{ width: '100%', padding: 0 }}>
        <Select
          key={state.reloadKey}
          label="Model"
          options={options}
          defaultValue={state.modelId}
          onChange={({ value }) => {
            dispatch({ modelId: value, label: '', success: false })
          }}
          isRequired={false}
          style={{ width: '100%' }}
        />

        <div css={{ height: spacing.base }} />

        <Combobox
          key={state.modelId}
          label="Label"
          options={() => getOptions({ modelId: state.modelId, projectId })}
          value={state.label}
          onChange={({ value }) => dispatch({ label: value, success: false })}
          hasError={state.errors.label !== undefined}
          errorMessage={state.errors.label}
        />

        <div css={{ height: spacing.comfy }} />

        <div css={{ display: 'flex' }}>
          <Button
            variant={BUTTON_VARIANTS.SECONDARY}
            onClick={() =>
              dispatch({
                modelId: localModelId,
                label: localLabel,
                reloadKey: state.reloadKey + 1,
              })
            }
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
            onClick={() =>
              onSubmit({
                dispatch,
                state,
                labels,
                projectId,
                assetId,
                setLocalModelId,
                setLocalLabel,
              })
            }
            isDisabled={
              (!state.modelId && !localModelId) ||
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
