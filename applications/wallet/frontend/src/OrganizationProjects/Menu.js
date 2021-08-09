import { useState } from 'react'
import PropTypes from 'prop-types'
import Link from 'next/link'
import Router from 'next/router'

import { fetcher, getQueryString } from '../Fetch/helpers'

import Menu from '../Menu'
import Button, { VARIANTS } from '../Button'
import ButtonActions from '../Button/Actions'
import ButtonExternal from '../Button/External'
import Modal from '../Modal'

const OrganizationProjectsMenu = ({
  organizationId,
  projectId,
  name,
  revalidate,
}) => {
  const [isDeleteModalOpen, setDeleteModalOpen] = useState(false)
  const [isDeleting, setIsDeleting] = useState(false)

  return (
    <>
      <Menu open="bottom-left" button={ButtonActions}>
        {({ onBlur, onClick }) => (
          <div>
            <ul>
              <li>
                <Link href={`/${projectId}`} passHref>
                  <Button
                    variant={VARIANTS.MENU_ITEM}
                    onBlur={onBlur}
                    onClick={onClick}
                  >
                    <ButtonExternal>View Project</ButtonExternal>
                  </Button>
                </Link>
              </li>

              <li>
                <Link href={`/${projectId}/users`} passHref>
                  <Button
                    variant={VARIANTS.MENU_ITEM}
                    onBlur={onBlur}
                    onClick={onClick}
                  >
                    <ButtonExternal>Manage Users</ButtonExternal>
                  </Button>
                </Link>
              </li>

              <li>
                <Link href={`/${projectId}/api-keys`} passHref>
                  <Button
                    variant={VARIANTS.MENU_ITEM}
                    onBlur={onBlur}
                    onClick={onClick}
                  >
                    <ButtonExternal>Manage API Keys</ButtonExternal>
                  </Button>
                </Link>
              </li>

              <li>
                <Button
                  variant={VARIANTS.MENU_ITEM}
                  onBlur={onBlur}
                  onClick={() => {
                    onClick()

                    setDeleteModalOpen(true)
                  }}
                >
                  Delete Project
                </Button>
              </li>
            </ul>
          </div>
        )}
      </Menu>

      {isDeleteModalOpen && (
        <Modal
          title="Delete Project"
          message={`Are you sure you want to delete the "${name}" project and remove all its content from the system? Deletion will be permanent and irreversible after 30 days.`}
          action={isDeleting ? 'Deleting...' : 'Delete Permanently'}
          onCancel={() => {
            setDeleteModalOpen(false)
          }}
          onConfirm={async () => {
            setIsDeleting(true)

            await fetcher(`/api/v1/projects/${projectId}/`, {
              method: 'DELETE',
            })

            setDeleteModalOpen(false)

            revalidate()

            const queryString = getQueryString({
              action: 'delete-project-success',
            })

            Router.push(
              `/organizations/[organizationId]${queryString}`,
              `/organizations/${organizationId}`,
            )
          }}
        />
      )}
    </>
  )
}

OrganizationProjectsMenu.propTypes = {
  organizationId: PropTypes.string.isRequired,
  projectId: PropTypes.string.isRequired,
  name: PropTypes.string.isRequired,
  revalidate: PropTypes.func.isRequired,
}

export default OrganizationProjectsMenu
