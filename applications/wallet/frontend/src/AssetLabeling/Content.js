import PropTypes from 'prop-types'
import useSWR from 'swr'
import Link from 'next/link'

import { colors, constants, spacing, typography } from '../Styles'

import Select, { VARIANTS as SELECT_VARIANTS } from '../Select'
import SuspenseBoundary from '../SuspenseBoundary'

import { useLabelTool } from './helpers'

import AssetLabelingForm from './Form'

const AssetLabelingContent = ({ projectId, assetId }) => {
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

      <div
        css={{
          padding: spacing.normal,
          borderBottom: constants.borders.regular.smoke,
        }}
      >
        <div
          css={{
            display: 'flex',
            justifyContent: 'space-between',
            fontWeight: typography.weight.bold,
          }}
        >
          <div>Select a dataset</div>
          <div>
            <Link href={`/${projectId}/datasets/add`} passHref>
              <a css={{ color: colors.key.two }}>+ New Data Set</a>
            </Link>
          </div>
        </div>

        <div css={{ height: spacing.base }} />

        <Select
          key={state.datasetId}
          label="Dataset:"
          options={datasets.map(({ id, name }) => ({ value: id, label: name }))}
          defaultValue={state.datasetId}
          onChange={({ value }) => {
            dispatch({ datasetId: value, labels: {} })
          }}
          isRequired={false}
          variant={SELECT_VARIANTS.COLUMN}
          style={{ width: '100%', backgroundColor: colors.structure.smoke }}
        />
      </div>

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
}

export default AssetLabelingContent
