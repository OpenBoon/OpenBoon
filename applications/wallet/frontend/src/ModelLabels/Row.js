import PropTypes from 'prop-types'

import ModelLabelsMenu from './Menu'

const ModelLabelsRow = ({
  projectId,
  modelId,
  label: { label, count },
  revalidate,
}) => {
  return (
    <tr>
      <td>{label}</td>

      <td>{count}</td>

      <td>
        <ModelLabelsMenu
          projectId={projectId}
          modelId={modelId}
          label={label}
          revalidate={revalidate}
        />
      </td>
    </tr>
  )
}

ModelLabelsRow.propTypes = {
  projectId: PropTypes.string.isRequired,
  modelId: PropTypes.string.isRequired,
  label: PropTypes.shape({
    label: PropTypes.string.isRequired,
    count: PropTypes.number.isRequired,
  }).isRequired,
  revalidate: PropTypes.func.isRequired,
}

export default ModelLabelsRow
