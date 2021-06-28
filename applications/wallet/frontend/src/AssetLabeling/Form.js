import PropTypes from 'prop-types'
import useSWR from 'swr'

import { spacing, constants, colors, typography } from '../Styles'

import Form from '../Form'
import FlashMessageErrors from '../FlashMessage/Errors'
import ButtonGroup from '../Button/Group'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import { decamelize } from '../Text/helpers'

import { getIsDisabled, getLabelState, onDelete, onSave } from './helpers'

import AssetLabelingShortcuts from './Shortcuts'
import AssetLabelingLabel from './Label'

const AssetLabelingForm = ({ projectId, assetId, state, dispatch }) => {
  const {
    data: { results: labels },
  } = useSWR(
    `/api/v1/projects/${projectId}/datasets/${state.datasetId}/label_tool_info/?assetId=${assetId}`,
  )

  const handleOnSave = () => {
    const isDisabled = getIsDisabled({ assetId, state, labels })

    if (isDisabled) return

    onSave({ projectId, assetId, state, dispatch })
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

  return (
    <div
      css={{ display: 'flex', flexDirection: 'column', flex: 1, height: '0%' }}
    >
      <AssetLabelingShortcuts onSave={handleOnSave} />

      <div
        css={{
          padding: spacing.normal,
          fontWeight: typography.weight.medium,
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
            styles={{ paddingTop: spacing.base, paddingBottom: spacing.comfy }}
          />

          {labels.map((label) => {
            const id = label.bbox ? JSON.stringify(label.bbox) : assetId

            const labelState = getLabelState({ id, state, labels })

            const labelDispatch = (value) => {
              dispatch({
                labels: {
                  ...(state.datasetType === 'Classification'
                    ? {}
                    : state.labels),
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

        <ButtonGroup>
          <Button
            variant={BUTTON_VARIANTS.SECONDARY}
            onClick={() => {
              dispatch({ datasetId: '', labels: {} })
            }}
            style={{ flex: 1, marginLeft: spacing.normal }}
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
      </Form>
    </div>
  )
}

AssetLabelingForm.propTypes = {
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
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
