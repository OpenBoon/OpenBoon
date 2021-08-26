import PropTypes from 'prop-types'
import useSWR from 'swr'

import { colors, constants, spacing, typography } from '../Styles'

import { useLabelTool } from '../AssetLabeling/helpers'

import AssetLabelingDataset from '../AssetLabeling/Dataset'
import SuspenseBoundary from '../SuspenseBoundary'
import Form from '../Form'
import ButtonGroup from '../Button/Group'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import BulkAssetLabelingForm from './Form'
import { onSave } from './helpers'

const BulkAssetLabeling = ({ projectId, query, setIsBulkLabeling }) => {
  const {
    data: { results: datasets },
  } = useSWR(`/api/v1/projects/${projectId}/datasets/all/`)

  const [state, dispatch] = useLabelTool({ projectId })

  const dataset = datasets.find(({ id }) => id === state.datasetId)

  const { type: datasetType = '' } = dataset || {}

  return (
    <>
      <div
        css={{
          padding: spacing.base,
          paddingLeft: spacing.normal,
          borderBottom: constants.borders.regular.smoke,
          color: colors.structure.pebble,
        }}
      >
        All Assets in Search
      </div>

      <AssetLabelingDataset
        projectId={projectId}
        assetId=""
        datasets={datasets}
      />

      <SuspenseBoundary>
        <Form style={{ width: '100%', padding: 0, overflow: 'hidden' }}>
          {state.datasetId ? (
            <BulkAssetLabelingForm
              projectId={projectId}
              datasetType={datasetType}
              state={state}
              dispatch={dispatch}
            />
          ) : (
            <div
              css={{
                padding: spacing.normal,
                color: colors.structure.white,
                fontStyle: typography.style.italic,
                borderBottom: constants.borders.regular.smoke,
              }}
            >
              Select a dataset to start labeling assets.
            </div>
          )}

          <div
            css={{
              paddingLeft: spacing.normal,
              paddingRight: spacing.normal,
            }}
          >
            <ButtonGroup>
              <Button
                variant={BUTTON_VARIANTS.SECONDARY}
                onClick={async () => {
                  await dispatch({
                    datasetId: '',
                    labels: {},
                    isLoading: false,
                    errors: {},
                  })
                  setIsBulkLabeling(false)
                }}
                style={{ flex: 1 }}
              >
                Cancel
              </Button>

              <Button
                type="submit"
                variant={BUTTON_VARIANTS.PRIMARY}
                onClick={() => {
                  onSave({ projectId, query, state, dispatch })
                }}
                isDisabled={
                  !state.datasetId || !state.lastLabel || !state.lastScope
                }
                style={{ flex: 1 }}
              >
                {state.isLoading ? 'Saving...' : 'Save'}
              </Button>
            </ButtonGroup>
          </div>
        </Form>
      </SuspenseBoundary>
    </>
  )
}

BulkAssetLabeling.propTypes = {
  projectId: PropTypes.string.isRequired,
  query: PropTypes.string.isRequired,
  setIsBulkLabeling: PropTypes.func.isRequired,
}

export default BulkAssetLabeling
