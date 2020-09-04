import PropTypes from 'prop-types'

import { colors, constants } from '../Styles'

import CheckmarkSvg from '../Icons/checkmark.svg'

import ModelLabelsMenu from './Menu'

const ModelLabelsRow = ({
  projectId,
  modelId,
  label: { label, count },
  revalidate,
  requiredAssetsPerLabel,
}) => {
  return (
    <tr>
      <td>{label}</td>

      <td css={{ textAlign: 'right' }}>{requiredAssetsPerLabel}</td>

      <td css={{ textAlign: 'right' }}>{count}</td>

      <td css={{ textAlign: 'right' }}>
        {count >= requiredAssetsPerLabel ? '-' : requiredAssetsPerLabel - count}
      </td>

      <td>
        {count >= requiredAssetsPerLabel && (
          <CheckmarkSvg
            height={constants.icons.regular}
            color={colors.signal.grass.base}
          />
        )}
      </td>

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
  requiredAssetsPerLabel: PropTypes.number.isRequired,
}

export default ModelLabelsRow
