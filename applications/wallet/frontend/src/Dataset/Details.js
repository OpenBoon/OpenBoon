import PropTypes from 'prop-types'
import useSWR from 'swr'

import { spacing, typography, colors } from '../Styles'

import { decamelize } from '../Text/helpers'

const LINE_HEIGHT = '23px'

const DatasetDetails = ({ projectId, datasetId }) => {
  const {
    data: { name, type, description },
  } = useSWR(`/api/v1/projects/${projectId}/datasets/${datasetId}/`)

  return (
    <div>
      <div
        css={{
          color: colors.structure.zinc,
          fontFamily: typography.family.condensed,
          textTransform: 'uppercase',
        }}
      >
        Dataset Name:
      </div>

      <h3
        css={{
          color: colors.structure.white,
          fontWeight: typography.weight.medium,
          fontSize: typography.size.giant,
          lineHeight: typography.height.giant,
          paddingBottom: spacing.normal,
        }}
      >
        {name}
      </h3>

      <ul
        css={{
          margin: 0,
          padding: 0,
          listStyle: 'none',
          lineHeight: LINE_HEIGHT,
        }}
      >
        <li>
          <span
            css={{
              color: colors.structure.zinc,
              fontFamily: typography.family.condensed,
              textTransform: 'uppercase',
            }}
          >
            Dataset Type:
          </span>{' '}
          {decamelize({ word: type })}
        </li>

        <li>
          <span
            css={{
              color: colors.structure.zinc,
              fontFamily: typography.family.condensed,
              textTransform: 'uppercase',
            }}
          >
            Description:
          </span>{' '}
          {description}
        </li>
      </ul>
    </div>
  )
}

DatasetDetails.propTypes = {
  projectId: PropTypes.string.isRequired,
  datasetId: PropTypes.string.isRequired,
}

export default DatasetDetails
