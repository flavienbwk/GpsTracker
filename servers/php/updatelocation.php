<?php
    include 'dbFile.php';
    include 'dbconnect.php';

    $dbFile = new dbFile("updates/updates_" . date("d_m_Y", time()) . ".json");
    $dbFile->add(json_encode($_POST));

    $latitude       = isset($_POST['latitude']) ? $_POST['latitude'] : '0';
    $latitude       = (float)str_replace(",", ".", $latitude); // to handle European locale decimals
    $longitude      = isset($_POST['longitude']) ? $_POST['longitude'] : '0';
    $longitude      = (float)str_replace(",", ".", $longitude);    
    $speed          = isset($_POST['speed']) ? $_POST['speed'] : 0;
    $direction      = isset($_POST['direction']) ? $_POST['direction'] : 0;
    $distance       = isset($_POST['distance']) ? $_POST['distance'] : '0';
    $distance       = (float)str_replace(",", ".", $distance);
    $date           = isset($_POST['date']) ? $_POST['date'] : '0000-00-00 00:00:00';
    $date           = urldecode($date);
    $locationmethod = isset($_POST['locationmethod']) ? $_POST['locationmethod'] : '';
    $locationmethod = urldecode($locationmethod);
    $username       = isset($_POST['username']) ? $_POST['username'] : 0;
    $phonenumber    = isset($_POST['phonenumber']) ? $_POST['phonenumber'] : '';
    $sessionid      = isset($_POST['sessionid']) ? $_POST['sessionid'] : 0;
    $accuracy       = isset($_POST['accuracy']) ? $_POST['accuracy'] : 0;
    $extrainfo      = isset($_POST['extrainfo']) ? $_POST['extrainfo'] : '';
    $eventtype      = isset($_POST['eventtype']) ? $_POST['eventtype'] : '';
    $deviceid      = isset($_POST['deviceid']) ? $_POST['deviceid'] : '';
    
    // doing some validation here
    if ($latitude == 0 && $longitude == 0) {
        exit('-1');
    }

    $params = array(':latitude'        => $latitude,
                    ':longitude'       => $longitude,
                    ':speed'           => $speed,
                    ':direction'       => $direction,
                    ':distance'        => $distance,
                    ':date'            => $date,
                    ':locationmethod'  => $locationmethod,
                    ':username'        => $username,
                    ':phonenumber'     => $phonenumber,
                    ':sessionid'       => $sessionid,
                    ':accuracy'        => $accuracy,
                    ':extrainfo'       => $extrainfo,
                    ':eventtype'       => $eventtype
                );

    switch ($dbType) {
        case DB_MYSQL:
            $stmt = $pdo->prepare( $sqlFunctionCallMethod.'prcSaveGPSLocation(
                          :latitude, 
                          :longitude, 
                          :speed, 
                          :direction, 
                          :distance, 
                          :date, 
                          :locationmethod,
                          :username, 
                          :phonenumber, 
                          :sessionid, 
                          :accuracy, 
                          :extrainfo, 
                          :eventtype);'
                      );
            break;
        case DB_POSTGRESQL:
        case DB_SQLITE3:
            $stmt = $pdo->prepare('INSERT INTO gpslocations (latitude, longitude, speed, direction, distance, gpsTime, locationMethod, userName, phoneNumber,  sessionID, accuracy, extraInfo, eventType) VALUES (:latitude, :longitude, :speed, :direction, :distance, :date, :locationmethod, :username, :phonenumber, :sessionid, :accuracy, :extrainfo, :eventtype)');
            break;
    }  
    $stmt->execute($params);
    $timestamp = $stmt->fetchColumn();
    echo $timestamp;
