import PropTypes from 'prop-types'
import Link from 'next/link'

import {colors, constants, spacing} from '../Styles'

import ImagesSvg from '../Icons/images.svg'
import DocumentsSvg from '../Icons/documents.svg'
import VideosSvg from '../Icons/videos.svg'

import {onRowClickRouterPush} from '../Table/helpers'
import {formatFullDate} from '../Date/helpers'

import DataSourcesMenu from './Menu'

const getSource = ({ uri }) => {
  if (uri.includes('s3://')) return 'Amazon Web Service (AWS)'

  if (uri.includes('azure://')) return 'Azure'

  if (uri.includes('gs://')) return 'Google Cloud Platform (GCP)'

  return 'Unknown'
}

const DataSourcesRow = ({
  projectId,
  dataSource: {
    id: dataSourceId,
    name,
    uri,
    timeCreated,
    timeModified,
    fileTypes,
  },
  revalidate,
}) => {
  return (
    <tr
      css={{ cursor: 'pointer' }}
      onClick={onRowClickRouterPush(
        '/[projectId]/data-sources/[dataSourceId]/edit',
        `/${projectId}/data-sources/${dataSourceId}/edit`,
      )}
    >
      <td>
        <Link
          href="/[projectId]/data-sources/[dataSourceId]/edit"
          as={`/${projectId}/data-sources/${dataSourceId}/edit`}
          passHref
        >
          <a css={{ ':hover': { textDecoration: 'none' } }}>{name}</a>
        </Link>
      </td>

      <td>{getSource({ uri })}</td>

      <td>{uri}</td>

      <td>{formatFullDate({ timestamp: timeCreated })}</td>

      <td>{formatFullDate({ timestamp: timeModified })}</td>

      <td>
        <div css={{ display: 'flex' }}>
          <ImagesSvg
            height={constants.icons.comfy}
            css={{ marginRight: spacing.normal }}
            color={
              fileTypes?.includes('Images')
                ? colors.signal.canary.base
                : colors.structure.transparent
            }
          />
          <DocumentsSvg
            height={constants.icons.comfy}
            css={{ marginRight: spacing.normal }}
            color={
              fileTypes?.includes('Documents')
                ? colors.graph.seafoam
                : colors.structure.transparent
            }
          />
          <VideosSvg
            height={constants.icons.comfy}
            color={
              fileTypes?.includes('Videos')
                ? colors.graph.iris
                : colors.structure.transparent
            }
          />
        </div>
      </td>

      <td>
        <DataSourcesMenu
          projectId={projectId}
          dataSourceId={dataSourceId}
          revalidate={revalidate}
        />
      </td>
    </tr>
  )
}

DataSourcesRow.propTypes = {
  projectId: PropTypes.string.isRequired,
  dataSource: PropTypes.shape({
    id: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired,
    uri: PropTypes.string.isRequired,
    timeCreated: PropTypes.number.isRequired,
    timeModified: PropTypes.number.isRequired,
    fileTypes: PropTypes.arrayOf(PropTypes.string.isRequired).isRequired,
  }).isRequired,
  revalidate: PropTypes.func.isRequired,
}

export default DataSourcesRow
