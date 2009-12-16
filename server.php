<?php
/*
	class geoVector
	{
		private $baseLat;
		private $baseLng;
		private $targetLat;
		private $targetLng;
		
		public function __construct($baseLat, $baseLng, $targetLat, $targetLng)
		{
			$this->$baseLat = $baseLat;
			$this->$baseLng = $baseLng;
			$this->$targetLat = $targetLat;
			$this->$targetLng = $targetLng;
		}
		
		public function isFraction($otherVector)
		{
			$test1 = ($this->$targetLat - $this->$baseLat) / $otherVector->$baseLat;
			$test2 = ($this->$targetLng - $this->$baseLng) / $otherVector->$baseLng;
			
			if($test1 == $test2)
				return true;
			
			error_log("Compare $test1 to $test2");
			return false;
		}
	}
	*/
	
	abstract class DatabaseTable
	{
		protected function __construct($connection, $tableName)
		{
			if($this->exists($connection, $tableName))
				return;
			
			$this->create($connection);
		}
		
		protected function exists($connection, $tableName)
		{
			return $connection->query("SELECT name from sqlite_master WHERE type = 'table' AND name = '$tableName'")->rowCount;
		}
	}

	class NodeTable extends DatabaseTable
	{	
		private function __construct($connection)
		{
			parent::__construct($connection, "NODE");
		}
		
		private function create($connection)
		{
			if(!$connection->beginTransaction())
				throw new Exception("Could not start transaction");
			
			if
			(
				!$connection->exec
				(
					"CREATE TABLE \"NODE\"
					(
						\"PLAYERID\" INTEGER NOT NULL,
						\"LAT\" REAL NOT NULL,
						\"LNG\" REAL NOT NULL,
						\"TIME\" INTEGER NOT NULL
					)"
				)
			)
			{
				$connection->rollBack();
				throw new Exception("Creating table Node failed");
			}
			
			if
			(
				!$connection->exec
				("CREATE UNIQUE INDEX \"node-id-lat-time\" on node (PLAYERID ASC, LAT ASC, TIME ASC)")
			)
			{
				$connection->rollBack();
				throw new Exception("Creating index failed");
			}
			
			if
			(
				!$connection->exec
				("CREATE UNIQUE INDEX \"node-lat\" on node (LAT ASC)")
			)
			{
				$connection->rollBack();
				throw new Exception("Creating index failed");
			}
			
			if(!$connection->commit())
			{
				$connection->rollback();
				throw new Exception("Commiting failed");
			}
		}
		
		private function isColliding($baseLat, $baseLng, $nextLat, $nextLng, $pointLat, $pointLng)
		{
			$xDistance = $nextLat - $baseLat;
			$yDistance = $nextLng - $baseLng;
			$distance =
				abs
				(
					$xDistance * ($baseLng - $pointLng)
					- ($baseLat - $pointLat) * $yDistance
				)
				/
				sqrt
				(
					pow($xDistance, 2) + pow($yDistance, 2)
				);
			/*
			$baseVector = new geoVector($baseLat, $baseLng, $nextLat, $nextLng);
			$pointVector = new geoVector($baseLat, $baseLng, $pointLat, $pointLng);
		
			return $baseVector->isFraction($pointVector);
			*/
			return $distance < 0.001;
		}
		
		public function collides($pointLat, $pointLng)
		{
			//brute force testing
			//get all players who have nodes
			$result = $connection->query("SELECT DISTINCT(PLAYERID) FROM NODE");
			
			$lastLat = null;
			$lastLng = null;
			while(($row = $result->fetch(SQLITE_NUM)))
			{
				$nodeResult = $connection->query("SELECT LAT, LNG FROM NODE WHERE PLAYERID = $row[0] ORDER BY TIME ASC");
				
				$nodeRow = $nodeResult->fetch(SQLITE_NUM);
				
				if(isColliding($lastLat, $lastLng, $nodeRow[0], $nodeRow[1], $pointLat, $pointLng))
					return true;
				
				$lastLat = $nodeRow[0];
				$lastLng = $nodeRow[1];
			}
			
			return false;
		}
		
		private $_nodeStatement;
		
		public function add($connection, $player, $lat, $lng)
		{
			$time = time();
			
			if(!$this->$_nodeStatement)
				$this->$_nodeStatement = $connection->prepare("INSERT INTO NODE (PLAYERID, LAT, LNG, TIME) VALUES (?, ?, ? , ?)");
			
			$this->$_nodeStatement->bindParam(1, "player", PDO::PARAM_INT);
			$this->$_nodeStatement->bindParam(2, "lat", PDO::PARAM_LOB);
			$this->$_nodeStatement->bindParam(3, "lng", PDO::PARAM_LOB);
			$this->$_nodeStatement->bindParam(4, "time", PDO::PARAM_INT);
			
			$this->$_nodeStatement->execute();
		}
		
		public function removeAll($connection)
		{
			$connection->exec("DELETE FROM NODE");
		}
		
		private $_nodeSelectStatement;
		
		public function getNew($connection, $playerId, $time)
		{
			if(!$this->$_nodeSelectStatement)
				$this->$_nodeSelectStatement = $connection->prepare
				(
					"SELECT
						PLAYERID,
						CONCAT('<node lat=\"', LAT, '\" lng=\"', LNG, '\"/>'),
						MAX(TIME)
					FROM NODE
					WHERE
						TIME >= ?
					 	AND PLAYERID <> ?
					ORDER BY PLAYERID, TIME"
				);
				
			$this->$_nodeSelectStatement->bindParam(1, "time", PDO::PARAM_INT);
			$this->$_nodeSelectStatement->bindParam(2, "playerId", PDO::PARAM_INT);
			$this->$_nodeSelectStatement->execute();

			$returnArray = array();
			//create the player array
			array_push($returnArray, array());
			//create the node array
			array_push($returnArray, array());
			//create the time entry
			array_push($returnArray, 0);
			
			$i = -1;
			while($row = $this->$_nodeSelectStatement->fetch(PDO::FETCH_NUM))
			{
				//check whether we have a new player
				if($row[0] != $lastPlayer)
				{
					$i++;
					array_push($returnArray[0], $row[0]);
					array_push($returnArray[1], "");
					$lastPlayer = $row[0];
				}

				//append node string
				$returnArray[1][i] += $returnArray[1][i] + $row[1];
				
				//set time
				$returnArray[2] = $row[2];
			}

			return $returnArray;
		}
		
		private static $nodes;
		public static function get($connection)
		{
			if(!self::$nodes)
				self::$nodes = new NodeTable($connection);
			
			return self::$nodes;
		}
	}
	
	class PlayerTable extends DatabaseTable
	{
		private function __construct($connection)
		{
			$parent->__construct($connection, "PLAYER");
		}
		
		public function add($connection, $playerName)
		{
			$connection->beginTransaction();
			$connection->exec("INSERT INTO PLAYER (NAME) VALUES ('$playerName')");
			$result = $connection->query("SELECT ID FROM PLAYER WHERE NAME = '$playerName'");
			$result->bindColumn(1, "id", PDO::PARAM_LOB);
			
			$result->fetch();
			
			$connection->commit();
			
			return $id;
		}
		
		public function remove($connection, $playerId)
		{
			$connection->exec("DELETE FROM PLAYER WHERE ID = $playerId;");
		}
		
		public function getCount($connection)
		{
			$result = $connection->query("SELECT DISTINCT(ID) FROM PLAYER");
			
			return $result->rowCount;
		}
		
		private $positionStatement;
		
		public function setPosition($connection, $playerId, $lat, $lng)
		{
			if(!$this->$positionStatement)
				$this->$positionStatement = $connection->prepare("UPDATE PLAYER SET LAT = ?, LNG = ? WHERE ID = ?");
			
			$this->$positionStatement->bindParam(1, "lat", PDO::PARAM_INPUT);
			$this->$positionStatement->bindParam(2, "lng", PDO::PARAM_INPUT);
			$this->$positionStatement->bindParam(3, "playerId", PDO::PARAM_INPUT);
			
			if(!$this->$positionStatement->execute())
				throw new Exception("Player $playerId position could not be updated");
		}
		
		private function create($connection)
		{
			$connection->exec
			(
				"CREATE TABLE PLAYER (
					ID INTEGER PRIMARY KEY NOT NULL,
					LAT REAL NULL,
					LNG REAL NULL,
					NAME TEXT NOT NULL
				);"
			);
		} 
		
		private static $players;
		
		public static function get($connection)
		{
			if(!self::$players)
				self::$players = new PlayerTable($connection);
			
			return self::$players;
		}
	}
	
	function updatePosition($method_name, $params, $connection)
	{
		$playerid = $params[0];
		$lat = $params[1];
		$lng = $params[2];
		
		if(!$lat || !$lng)
			return createFault(1, "updatePosition needs a position");
		
		PlayerTable::get($connection)->setPosition($connection, $playerid, $lat, $lng);
	}
	
	function createFault($code, $string)
	{
		return array("faultCode" => $code, "faultString" => $string);
	}

	function addPlayer($method_name, $params, $connection)
	{
		$name = $params[0];
		
		if(!$name)
			return createFault(1, "addPlayer called without player name");
		
		$id = PlayerTable::get($connection)->add($connection, $name);
		
		if(!$id)
			return createFault(1, "addPlayer could not create new player");
		
		return (int)$id;
	}
	
	function endGame($connection)
	{
		NodeTable::get($connection)->removeAll();
	}
	
	function removePlayer($method_name, $params, $connection)
	{
		$id = $params[0];
		
		if(!$id)
			return createFault(1, "removePlayer called without id");
		
		$table = PlayerTable::get($connection);
		$table->remove($connection, $id);
		
		if($table->getCount($connection) == 0)
			endGame($connection);
	}
	
	function getNodes($method_name, $params, $connection)
	{
		$playerid = $params[0];
		$time = $params[1];
		
		if(!$time)
			$time = mktime(0, 0, 0, 1, 1, 1970);
		
		$currentTime = time();
		
		$result = NodeTable::get($connection)->getNew($playerid, $time);
		
		$return = "<state time=\"$result[2]\"/>";
		
		for($i = 0; count($result[0]); $i++)
		{
			$return += "<player id=\"$result[0][i]\">$result[1][i]</player>";
		}
		
		xmlrpc_set_type($return, "string");
		return $return;
	}
	
	function addNode($method_name, $params, $connection)
	{
		$playerid = $params[0];
		NodeTable::get($connection)->add($playerid);
	}
	
	class SessionHandler
	{
		public static function create($path, $sessionName)
		{

		}
		
		public static function destroy()
		{
			
		}
	}
	
	session_set_save_handler(SessionHandler::create, SessionHandler::destroy, callback $read , callback $write , callback $destroy , callback $gc )

	$database = new PDO("sqlite:node.db", null, null, array(PDO::ATTR_PERSISTENT => true));
	
	$xmlrpc_server = xmlrpc_server_create();
	$registered1 = xmlrpc_server_register_method($xmlrpc_server, "addPlayer", "addPlayer");
	$registered2 = xmlrpc_server_register_method($xmlrpc_server, "removePlayer", "removePlayer");
	$registered3 = xmlrpc_server_register_method($xmlrpc_server, "getNodes", "getNodes");
	$registered4 = xmlrpc_server_register_method($xmlrpc_server, "addNode", "addNode");
	$registered5 = xmlrpc_server_register_method($xmlrpc_server, "updatePosition", "updatePosition");
	
	$post = file_get_contents("php://input");
	//error_log("post is $post");
	$response = xmlrpc_server_call_method($xmlrpc_server, $post, $database, array());
	
	//error_log("response is $response");

	// Set content type to text/xml
	header('Content-Type: text/xml');
	
	print $response;
	xmlrpc_server_destroy($xmlrpc_server);
?>
