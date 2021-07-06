import PropTypes from 'prop-types'
import useSWR from 'swr'

import { spacing } from '../Styles'

import SuspenseBoundary from '../SuspenseBoundary'
import { decode } from '../Filters/helpers'
import { SCOPE_OPTIONS } from '../AssetLabeling/helpers'

import DatasetLabelsSelection from './Selection'
import DatasetLabelsAssets from './Assets'

const DatasetLabelsContent = ({
  projectId,
  datasetId,
  query,
  page,
  datasetName,
}) => {
  const {
    data: { results: labels = [] },
  } = useSWR(`/api/v1/projects/${projectId}/datasets/${datasetId}/get_labels/`)

  const { scope = SCOPE_OPTIONS[0].value, label = '#All#' } = decode({ query })

  return (
    <>
      <DatasetLabelsSelection
        projectId={projectId}
        datasetId={datasetId}
        datasetName={datasetName}
        scope={scope}
        label={label}
        labels={labels}
      />

      <div css={{ height: spacing.normal }} />

      <SuspenseBoundary>
        <DatasetLabelsAssets
          projectId={projectId}
          datasetId={datasetId}
          page={page}
          datasetName={datasetName}
          scope={scope}
          label={label}
          labels={labels}
        />
      </SuspenseBoundary>
    </>
  )
}

DatasetLabelsContent.propTypes = {
  projectId: PropTypes.string.isRequired,
  datasetId: PropTypes.string.isRequired,
  query: PropTypes.string.isRequired,
  page: PropTypes.number.isRequired,
  datasetName: PropTypes.string.isRequired,
}

export default DatasetLabelsContent
