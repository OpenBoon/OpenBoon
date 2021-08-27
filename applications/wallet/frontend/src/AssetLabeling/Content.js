import { useEffect } from 'react'
import PropTypes from 'prop-types'
import useSWR from 'swr'

import { colors, constants, spacing, typography } from '../Styles'

import SuspenseBoundary from '../SuspenseBoundary'

import { useLabelTool } from './helpers'

import AssetLabelingDataset from './Dataset'
import AssetLabelingForm from './Form'

const AssetLabelingContent = ({ projectId, assetId, setIsBulkLabeling }) => {
  const {
    data: { results: datasets },
  } = useSWR(`/api/v1/projects/${projectId}/datasets/all/`)

  const {
    data: {
      metadata: {
        source: { filename },
        analysis: { 'boonai-face-detection': { predictions } = {} } = {},
      },
    },
  } = useSWR(`/api/v1/projects/${projectId}/assets/${assetId}/`)

  const [state, dispatch] = useLabelTool({ projectId })

  useEffect(() => {
    setIsBulkLabeling(false)
    dispatch({ labels: {}, isLoading: false, errors: {} })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [assetId])

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
        {filename}
      </div>

      <AssetLabelingDataset
        projectId={projectId}
        assetId={assetId}
        datasets={datasets}
      />

      <SuspenseBoundary>
        {state.datasetId ? (
          <AssetLabelingForm
            projectId={projectId}
            assetId={assetId}
            hasFaceDetection={!!predictions}
            state={{ ...state, datasetType }}
            dispatch={dispatch}
          />
        ) : (
          <div
            css={{
              padding: spacing.normal,
              color: colors.structure.white,
              fontStyle: typography.style.italic,
            }}
          >
            Select a dataset to start labeling assets.
          </div>
        )}
      </SuspenseBoundary>
    </>
  )
}

AssetLabelingContent.propTypes = {
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  setIsBulkLabeling: PropTypes.func.isRequired,
}

export default AssetLabelingContent
