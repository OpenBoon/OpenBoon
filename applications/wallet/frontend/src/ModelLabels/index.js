import { useRouter } from 'next/router'
import { PropTypes } from 'prop-types'

import Table from '../Table'

import ModelLabelsRow from './Row'

const ModelLabels = ({ requiredAssetsPerLabel }) => {
  const {
    query: { projectId, modelId },
  } = useRouter()

  return (
    <Table
      legend="Labels"
      url={`/api/v1/projects/${projectId}/models/${modelId}/get_labels/`}
      refreshKeys={[]}
      refreshButton={false}
      columns={[
        'Label',
        '# Required',
        '# Labeled',
        '# Remaining',
        '#Checkmark#',
        '#Actions#',
      ]}
      expandColumn={1}
      renderEmpty="There are currently no labels for this model."
      renderRow={({ result, revalidate }) => (
        <ModelLabelsRow
          key={result.label}
          projectId={projectId}
          modelId={modelId}
          label={result}
          revalidate={revalidate}
          requiredAssetsPerLabel={requiredAssetsPerLabel}
        />
      )}
    />
  )
}

ModelLabels.propTypes = {
  requiredAssetsPerLabel: PropTypes.number.isRequired,
}

export default ModelLabels
