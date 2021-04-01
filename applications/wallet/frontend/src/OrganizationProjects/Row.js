import PropTypes from 'prop-types'

import { formatUsage } from '../Project/helpers'

import OrganizationProjectsMenu from './Menu'

const OrganizationProjectsRow = ({
  organizationId,
  project: {
    id,
    name,
    mlUsageThisMonth: {
      tier1: {
        imageCount: internalImageCount,
        videoMinutes: internalVideoMinutes,
      },
      tier2: {
        imageCount: externalImageCount,
        videoMinutes: externalVideoMinutes,
      },
    },
    totalStorageUsage: {
      imageCount: totalImageCount,
      videoMinutes: totalVideoMinutes,
    },
  },
  revalidate,
}) => {
  return (
    <tr>
      <td>{name}</td>
      <td>{formatUsage({ number: internalImageCount })}</td>
      <td>{formatUsage({ number: externalImageCount })}</td>
      <td>{formatUsage({ number: totalImageCount })}</td>
      <td>{formatUsage({ number: internalVideoMinutes / 60 })}</td>
      <td>{formatUsage({ number: externalVideoMinutes / 60 })}</td>
      <td>{formatUsage({ number: totalVideoMinutes / 60 })}</td>
      <td>
        <OrganizationProjectsMenu
          organizationId={organizationId}
          projectId={id}
          revalidate={revalidate}
        />
      </td>
    </tr>
  )
}

OrganizationProjectsRow.propTypes = {
  organizationId: PropTypes.string.isRequired,
  project: PropTypes.shape({
    id: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired,
    mlUsageThisMonth: PropTypes.shape({
      tier1: PropTypes.shape({
        imageCount: PropTypes.number.isRequired,
        videoMinutes: PropTypes.number.isRequired,
      }).isRequired,
      tier2: PropTypes.shape({
        imageCount: PropTypes.number.isRequired,
        videoMinutes: PropTypes.number.isRequired,
      }).isRequired,
    }).isRequired,
    totalStorageUsage: PropTypes.shape({
      imageCount: PropTypes.number.isRequired,
      videoMinutes: PropTypes.number.isRequired,
    }).isRequired,
  }).isRequired,
  revalidate: PropTypes.func.isRequired,
}

export default OrganizationProjectsRow
