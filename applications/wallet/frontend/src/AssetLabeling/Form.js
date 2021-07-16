import PropTypes from 'prop-types'
import useSWR from 'swr'

import { spacing, constants, colors, typography } from '../Styles'

import Form from '../Form'
import FlashMessageErrors from '../FlashMessage/Errors'
import ButtonGroup from '../Button/Group'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import { decamelize } from '../Text/helpers'

import {
  getIsDisabled,
  getLabelState,
  onSave,
  onDelete,
  onFaceDetect,
} from './helpers'

import AssetLabelingShortcuts from './Shortcuts'
import AssetLabelingLabel from './Label'

const AssetLabelingForm = ({
  projectId,
  assetId,
  hasFaceDetection,
  state,
  dispatch,
}) => {
  const {
    data: { results: labels },
  } = useSWR(
    `/api/v1/projects/${projectId}/datasets/${state.datasetId}/label_tool_info/?assetId=${assetId}`,
  )

  const handleOnSave = () => {
    const isDisabled = getIsDisabled({ assetId, state, labels })

    if (isDisabled) return

    onSave({ projectId, assetId, state, labels, dispatch })
  }

  const handleOnDelete = ({ label }) => {
    onDelete({
      projectId,
      datasetId: state.datasetId,
      assetId,
      dispatch,
      labels: state.labels,
      label,
    })
  }

  if (state.datasetType === 'FaceRecognition' && !hasFaceDetection) {
    return (
      <div
        css={{
          display: 'flex',
          flexDirection: 'column',
          flex: 1,
          height: '0%',
          padding: spacing.normal,
        }}
      >
        <FlashMessageErrors
          errors={state.errors}
          styles={{ paddingBottom: spacing.normal, div: { flex: 1 } }}
        />

        <div
          css={{
            paddingBottom: spacing.normal,
            fontWeight: typography.weight.bold,
          }}
        >
          Add {decamelize({ word: state.datasetType })} Labels
        </div>

        <i css={{ color: colors.structure.zinc }}>
          Face detection analysis has not been run on this asset. You can add
          &quot;boonai-face-detection&quot; to the Data Source or you can run it
          just on this asset by clicking the button below.
        </i>

        <ButtonGroup>
          <Button
            variant={BUTTON_VARIANTS.PRIMARY}
            onClick={() => {
              onFaceDetect({
                projectId,
                datasetId: state.datasetId,
                assetId,
                dispatch,
              })
            }}
            style={{ flex: 1 }}
          >
            {state.isLoading
              ? 'Running...'
              : 'Run Face Detection On This Asset'}
          </Button>
        </ButtonGroup>
      </div>
    )
  }

  if (
    state.datasetType === 'FaceRecognition' &&
    hasFaceDetection &&
    labels.length === 0
  ) {
    return (
      <div
        css={{
          display: 'flex',
          flexDirection: 'column',
          flex: 1,
          height: '0%',
          padding: spacing.normal,
        }}
      >
        <FlashMessageErrors
          errors={state.errors}
          styles={{ paddingBottom: spacing.normal, div: { flex: 1 } }}
        />

        <div
          css={{
            paddingBottom: spacing.normal,
            fontWeight: typography.weight.bold,
          }}
        >
          Add {decamelize({ word: state.datasetType })} Labels
        </div>

        <i css={{ color: colors.structure.zinc }}>
          No Faces have been detected in this Asset.
        </i>
      </div>
    )
  }

  return (
    <div
      css={{ display: 'flex', flexDirection: 'column', flex: 1, height: '0%' }}
    >
      <AssetLabelingShortcuts onSave={handleOnSave} />

      <div
        css={{
          padding: spacing.normal,
          fontWeight: typography.weight.bold,
        }}
      >
        Add {decamelize({ word: state.datasetType })} Labels
      </div>

      <Form style={{ width: '100%', padding: 0, overflow: 'hidden' }}>
        <div
          css={{
            borderBottom: constants.borders.regular.smoke,
            backgroundColor: colors.structure.coal,
            overflow: 'auto',
          }}
        >
          <FlashMessageErrors
            errors={state.errors}
            styles={{ padding: spacing.normal, div: { flex: 1 } }}
          />

          {labels.map((label) => {
            const id = label.bbox ? JSON.stringify(label.bbox) : assetId

            const labelState = getLabelState({ id, state, labels })

            const labelDispatch = (value) => {
              dispatch({
                lastLabel: value.label,
                lastScope: value.scope,
                labels: {
                  ...state.labels,
                  [id]: value,
                },
              })
            }

            return (
              <AssetLabelingLabel
                key={id}
                projectId={projectId}
                datasetId={state.datasetId}
                state={labelState}
                dispatch={labelDispatch}
                label={label}
                onDelete={handleOnDelete}
              />
            )
          })}
        </div>

        <div
          css={{ paddingLeft: spacing.normal, paddingRight: spacing.normal }}
        >
          <ButtonGroup>
            <Button
              variant={BUTTON_VARIANTS.SECONDARY}
              onClick={() => {
                dispatch({ datasetId: '', labels: {} })
              }}
              style={{ flex: 1 }}
            >
              Cancel
            </Button>

            <Button
              type="submit"
              variant={BUTTON_VARIANTS.PRIMARY}
              onClick={handleOnSave}
              isDisabled={getIsDisabled({ assetId, state, labels })}
              style={{ flex: 1 }}
            >
              {state.isLoading ? 'Saving...' : 'Save'}
            </Button>
          </ButtonGroup>
        </div>
      </Form>
    </div>
  )
}

AssetLabelingForm.propTypes = {
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  hasFaceDetection: PropTypes.bool.isRequired,
  state: PropTypes.shape({
    datasetId: PropTypes.string.isRequired,
    datasetType: PropTypes.string.isRequired,
    isLoading: PropTypes.bool.isRequired,
    labels: PropTypes.shape({}).isRequired,
    errors: PropTypes.shape({
      global: PropTypes.string,
    }).isRequired,
  }).isRequired,
  dispatch: PropTypes.func.isRequired,
}

export default AssetLabelingForm
