import { useReducer } from 'react'
import PropTypes from 'prop-types'

import modelShape from '../Model/shape'

import { spacing } from '../Styles'

import { useLocalStorage } from '../LocalStorage/helpers'
import Form from '../Form'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import FlashMessageErrors from '../FlashMessage/Errors'
import Select, { VARIANTS as SELECT_VARIANTS } from '../Select'
import Combobox from '../Combobox'

import { onSubmit, getSubmitText, getOptions, SCOPE_OPTIONS } from './helpers'

import AssetLabelingShortcuts from './Shortcuts'

const INITIAL_STATE = {
  success: false,
  isLoading: false,
  errors: {},
  reloadKey: 0,
}

const reducer = (state, action) => ({ ...state, ...action })

const AssetLabelingAdd = ({ projectId, assetId, models, labels }) => {
  const [
    { modelId, label, scope, assetId: storedAssetId },
    dispatch,
  ] = useLocalStorage({
    key: `AssetLabelingAdd.${projectId}`,
    reducer,
    initialState: {
      modelId: '',
      label: '',
      scope: SCOPE_OPTIONS[0].value,
      assetId,
    },
  })

  const hasModel = models.find(({ id }) => id === modelId)

  const [localState, localDispatch] = useReducer(reducer, {
    ...INITIAL_STATE,
    modelId: (hasModel && modelId) || '',
    label: label || '',
    scope: scope || SCOPE_OPTIONS[0].value,
  })

  const options = models.map(({ name, id }) => ({ value: id, label: name }))

  const existingLabel = !!labels.find(
    (l) =>
      l.modelId === localState.modelId &&
      l.scope === localState.scope &&
      l.label === localState.label,
  )

  const handleOnSubmit = () => {
    onSubmit({
      dispatch,
      localDispatch,
      localState,
      labels,
      projectId,
      assetId,
    })
  }

  return (
    <div css={{ padding: spacing.normal }}>
      <AssetLabelingShortcuts onSubmit={handleOnSubmit} />

      <Form style={{ width: '100%', padding: 0 }}>
        <FlashMessageErrors
          errors={localState.errors}
          styles={{ paddingTop: spacing.base, paddingBottom: spacing.comfy }}
        />
        <Select
          key={`model${localState.reloadKey}`}
          label="Model"
          options={options}
          defaultValue={localState.modelId}
          onChange={({ value }) => {
            localDispatch({ modelId: value, label: '', success: false })
          }}
          isRequired={false}
          variant={SELECT_VARIANTS.COLUMN}
          style={{ width: '100%' }}
        />

        <div css={{ height: spacing.base }} />

        <Combobox
          key={`label${localState.reloadKey}${localState.modelId}`}
          label="Label"
          options={() => getOptions({ projectId, modelId: localState.modelId })}
          value={localState.label}
          onChange={({ value }) => {
            localDispatch({ label: value, success: false })
          }}
          hasError={localState.errors.label !== undefined}
          errorMessage={localState.errors.label}
        />

        <div css={{ height: spacing.base }} />

        <Select
          key={`scope${localState.reloadKey}`}
          label="Scope"
          options={SCOPE_OPTIONS}
          defaultValue={localState.scope}
          onChange={({ value }) => {
            localDispatch({ scope: value, success: false })
          }}
          isRequired={false}
          variant={SELECT_VARIANTS.COLUMN}
          style={{ width: '100%' }}
        />

        <div css={{ height: spacing.comfy }} />

        <div css={{ display: 'flex' }}>
          <Button
            variant={BUTTON_VARIANTS.SECONDARY}
            onClick={() => {
              localDispatch({
                modelId,
                label,
                scope,
                reloadKey: localState.reloadKey + 1,
              })
            }}
            style={{ flex: 1, margin: 0 }}
            /*
             * Cancel should only be enabled when a user has made changes
             * that differs from the localStorage values
             *
             * assetId is checked so Cancel is properly disabled when users navigate between assets
             */
            isDisabled={
              !modelId || !label || assetId !== storedAssetId || existingLabel
            }
          >
            Cancel
          </Button>

          <div css={{ padding: spacing.base }} />

          <Button
            type="submit"
            variant={BUTTON_VARIANTS.PRIMARY}
            onClick={handleOnSubmit}
            isDisabled={
              existingLabel ||
              !localState.modelId ||
              !localState.label ||
              !localState.scope ||
              localState.isLoading ||
              (localState.success && !localState.isLoading)
            }
            style={{ flex: 1, margin: 0 }}
          >
            {getSubmitText({ localState, existingLabel })}
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
